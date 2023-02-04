# -*- coding: utf-8 -*-
import datetime
import hamcrest
import pytest
import copy
from decimal import Decimal

from balance import balance_db as db
from balance import balance_steps as steps
from balance import balance_api as api
from btestlib import utils, reporter
from dateutil.relativedelta import relativedelta
from btestlib.constants import Products, Paysyses, Services, Firms, ContractPaymentType, ContractCommissionType, \
    ContractCreditType, Collateral, Users
from balance.features import Features
from btestlib.data.defaults import Date
import json
from hamcrest import equal_to
import btestlib.data.defaults as defaults
from btestlib.matchers import has_entries_casted, matches_in_time

LAST_DAY_OF_PREVIOUS_MONTH = datetime.datetime.now().replace(day=1) - datetime.timedelta(days=1)


def set_overdraft_params(person_id, client_limit, service_id=7, payment_method='bank', iso_currency='RUB'):
    return api.medium().SetOverdraftParams({'PersonID': person_id, 'ServiceID': service_id,
                                            'PaymentMethodCC': payment_method, 'ClientLimit': client_limit,
                                            'Currency': iso_currency})  # Currency, а не IsoCurrency


class ClientNotification(object):
    DEFAULT_VALUES = {'BusinessUnit': '0',
                      'CanBeForceCurrencyConverted': '0',
                      # 'ClientCurrency': 'RUB',
                      # 'ClientID': client_id,
                      # 'MigrateToCurrencyDone': '1',
                      # TODO: убираем проверку: слишком сложная (нужен соответствующий Биллингу учёт праздников)
                      # 'MinPaymentTerm': '0000-00-00',
                      # 'NonResident': '0',
                      # 'OverdraftBan': '0',
                      # 'OverdraftLimit': '0',
                      # 'OverdraftSpent': '0.00',
                      # 'Tid': '20160420140303745'
                      }

    def __init__(self, parameters={}):
        self.values = copy.deepcopy(ClientNotification.DEFAULT_VALUES)
        self.values.update(parameters)


def prepare_client(limit):
    client_id = steps.ClientSteps.create()
    # Переходим на мультивалютность миграцией
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2))

    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id
    paysys_id = Paysyses.BANK_UR_RUB.id

    # now = datetime.datetime.now()
    now = LAST_DAY_OF_PREVIOUS_MONTH

    # Разрешаем овердрафт
    steps.OverdraftSteps.set_force_overdraft(client_id, service_id, limit, Firms.YANDEX_1.id, currency='RUB')

    person_id = steps.PersonSteps.create(client_id, 'ur')

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product_id, params={'AgencyID': None})

    # Кладем на заказы равное количество денег
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 1500}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
                                                 contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    # Откурчиваем все по каждому заказу
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Money': 1500}, 0, now - relativedelta(days=1))

    act_id = steps.ActsSteps.generate(client_id, force=1, date=now)[0]

    print steps.CommonSteps.build_notification(1, object_id=order_id)['args'][0].get('TotalConsumeQty')

    return client_id, person_id, service_id, product_id, paysys_id, service_id, service_order_id, request_id, \
           invoice_id, order_id


def test_client():
    prepare_client(1000)


# Техсвязки
# 1, 2, 3
def test_overdraft_brand_client_currency():
    dt = datetime.datetime.now() - relativedelta(days=1)
    contract_dt = dt - relativedelta(days=1)
    client_limit = 10000
    service_id = Services.DIRECT.id

    client_id_1 = steps.ClientSteps.create()
    person_id_1 = steps.PersonSteps.create(client_id_1, 'ur')

    client_id_2 = steps.ClientSteps.create()
    person_id_2 = steps.PersonSteps.create(client_id_2, 'ur')

    # Переходим на мультивалютность миграцией
    steps.ClientSteps.migrate_to_currency(client_id_1, currency_convert_type='MODIFY',
                                          dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2))
    steps.ClientSteps.migrate_to_currency(client_id_2, currency_convert_type='MODIFY',
                                          dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2))

    # Разрешаем овердрафт
    steps.OverdraftSteps.set_force_overdraft(client_id_1, service_id, client_limit, Firms.YANDEX_1.id, currency='RUB')
    # steps.OverdraftSteps.set_force_overdraft(client_id_2, service_id, client_limit, Firms.YANDEX_1.id, currency='RUB')

    contract_params = {
        'CLIENT_ID': client_id_1,
        'PERSON_ID': person_id_1,
        'SERVICES': [Services.DIRECT.id],
        'DT': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt + relativedelta(days=3))),
        'IS_SIGNED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt)),
        'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY_ISO,
        'BRAND_TYPE': 7,
        'BRAND_CLIENTS': json.dumps([{"id": "1", "num": client_id_1, "client": client_id_1},
                                     {"id": "2", "num": client_id_2, "client": client_id_2}])
    }

    brand_contract_id, contract_external_id = steps.ContractSteps.create_contract_new(ContractCommissionType.BRAND,
                                                                                      contract_params)

    contract_dt = utils.Date.nullify_time_of_date(contract_dt)

    #  Сейчас дата начала договора в будущем (иначе договор не создается), апдейтом меняем ее на прошлое
    with reporter.step(u'Меняем дату начала договора на {date}'.format(date=contract_dt)):
        db.balance().execute("UPDATE t_contract_collateral SET dt = :dt WHERE contract2_id = :contract_id",
                             {'dt': contract_dt, 'contract_id': brand_contract_id})

    # После каждого апдейта по договору о тех. связке нужно рефрешнуть матвью
    db.balance().execute("BEGIN dbms_mview.refresh('BO.mv_client_direct_brand','C'); END;")


def test_overdraft_brand_client_fish():
    dt = datetime.datetime.now() - relativedelta(days=1)
    contract_dt = dt - relativedelta(days=1)
    client_limit = 10000
    service_id = Services.DIRECT.id

    client_id_1 = steps.ClientSteps.create()
    person_id_1 = steps.PersonSteps.create(client_id_1, 'ur')

    client_id_2 = steps.ClientSteps.create()
    person_id_2 = steps.PersonSteps.create(client_id_2, 'ur')


    # Разрешаем овердрафт
    steps.OverdraftSteps.set_force_overdraft(client_id_1, service_id, client_limit, Firms.YANDEX_1.id)
    # steps.OverdraftSteps.set_force_overdraft(client_id_2, service_id, client_limit, Firms.YANDEX_1.id, currency='RUB')

    contract_params = {
        'CLIENT_ID': client_id_1,
        'PERSON_ID': person_id_1,
        'SERVICES': [Services.DIRECT.id],
        'DT': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt + relativedelta(days=3))),
        'IS_SIGNED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt)),
        'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY_ISO,
        'BRAND_TYPE': 7,
        'BRAND_CLIENTS': json.dumps([{"id": "1", "num": client_id_1, "client": client_id_1},
                                     {"id": "2", "num": client_id_2, "client": client_id_2}])
    }

    brand_contract_id, contract_external_id = steps.ContractSteps.create_contract_new(ContractCommissionType.BRAND,
                                                                                      contract_params)

    contract_dt = utils.Date.nullify_time_of_date(contract_dt)

    #  Сейчас дата начала договора в будущем (иначе договор не создается), апдейтом меняем ее на прошлое
    with reporter.step(u'Меняем дату начала договора на {date}'.format(date=contract_dt)):
        db.balance().execute("UPDATE t_contract_collateral SET dt = :dt WHERE contract2_id = :contract_id",
                             {'dt': contract_dt, 'contract_id': brand_contract_id})

    # После каждого апдейта по договору о тех. связке нужно рефрешнуть матвью
    db.balance().execute("BEGIN dbms_mview.refresh('BO.mv_client_direct_brand','C'); END;")


# 4
def test_brand_contract_unable_with_autooverdraft():
    dt = datetime.datetime.now() - relativedelta(days=1)
    contract_dt = dt - relativedelta(days=1)
    client_limit = 10000
    service_id = Services.DIRECT.id

    client_id_1 = steps.ClientSteps.create()
    person_id_1 = steps.PersonSteps.create(client_id_1, 'ur')

    client_id_2 = steps.ClientSteps.create()
    person_id_2 = steps.PersonSteps.create(client_id_2, 'ur')

    # Переходим на мультивалютность миграцией
    steps.ClientSteps.migrate_to_currency(client_id_1, currency_convert_type='MODIFY',
                                          dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2))
    steps.ClientSteps.migrate_to_currency(client_id_2, currency_convert_type='MODIFY',
                                          dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2))

    # Разрешаем овердрафт
    steps.OverdraftSteps.set_force_overdraft(client_id_2, service_id, client_limit, Firms.YANDEX_1.id, currency='RUB')

    # Разрешаем автоовердрафт
    set_overdraft_params(person_id=person_id_2, client_limit=client_limit)

    contract_params = {
        'CLIENT_ID': client_id_1,
        'PERSON_ID': person_id_1,
        'SERVICES': [Services.DIRECT.id],
        'DT': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt + relativedelta(days=3))),
        'IS_SIGNED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt)),
        'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY_ISO,
        'BRAND_TYPE': 7,
        'BRAND_CLIENTS': json.dumps([{"id": "1", "num": client_id_1, "client": client_id_1},
                                     {"id": "2", "num": client_id_2, "client": client_id_2}])
    }

    try:
        brand_contract_id, contract_external_id = steps.ContractSteps.create_contract_new(ContractCommissionType.BRAND,
                                                                                          contract_params)
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc), equal_to('CONTRACT_RULE_VIOLATION'))
        utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                         equal_to(u"Rule violation: 'Клиент(ы) " + str(client_id_2) +
                                  u" имеют автоовердрафт. Включение их в бренд запрещено.'"))
        pass
    else:
        assert 1 == 2

    contract_params = {
        'CLIENT_ID': client_id_2,
        'PERSON_ID': person_id_2,
        'SERVICES': [Services.DIRECT.id],
        'DT': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt + relativedelta(days=3))),
        'IS_SIGNED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt)),
        'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY_ISO,
        'BRAND_TYPE': 7,
        'BRAND_CLIENTS': json.dumps([{"id": "1", "num": client_id_1, "client": client_id_1},
                                     {"id": "2", "num": client_id_2, "client": client_id_2}])
    }

    try:
        brand_contract_id, contract_external_id = steps.ContractSteps.create_contract_new(ContractCommissionType.BRAND,
                                                                                          contract_params)
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc), equal_to('CONTRACT_RULE_VIOLATION'))
        utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
                         equal_to(u"Rule violation: 'Клиент(ы) " + str(client_id_2) +
                                  u" имеют автоовердрафт. Включение их в бренд запрещено.'"))
        pass
    else:
        assert 1 == 2


# 5
def test_brand_contract_enable_with_zero_autooverdraft():
    dt = datetime.datetime.now() - relativedelta(days=1)
    contract_dt = dt - relativedelta(days=1)
    client_limit = 10000
    service_id = Services.DIRECT.id

    client_id_1 = steps.ClientSteps.create()
    person_id_1 = steps.PersonSteps.create(client_id_1, 'ur')

    client_id_2 = steps.ClientSteps.create()
    person_id_2 = steps.PersonSteps.create(client_id_2, 'ur')

    # Переходим на мультивалютность миграцией
    steps.ClientSteps.migrate_to_currency(client_id_1, currency_convert_type='MODIFY',
                                          dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2))
    steps.ClientSteps.migrate_to_currency(client_id_2, currency_convert_type='MODIFY',
                                          dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2))

    # Разрешаем овердрафт
    steps.OverdraftSteps.set_force_overdraft(client_id_2, service_id, client_limit, Firms.YANDEX_1.id, currency='RUB')

    client_limit = 0
    # Разрешаем автоовердрафт
    set_overdraft_params(person_id=person_id_2, client_limit=client_limit)

    contract_params = {
        'CLIENT_ID': client_id_1,
        'PERSON_ID': person_id_1,
        'SERVICES': [Services.DIRECT.id],
        'DT': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt + relativedelta(days=3))),
        'IS_SIGNED': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt)),
        'FINISH_DT': Date.HALF_YEAR_AFTER_TODAY_ISO,
        'BRAND_TYPE': 7,
        'BRAND_CLIENTS': json.dumps([{"id": "1", "num": client_id_1, "client": client_id_1},
                                     {"id": "2", "num": client_id_2, "client": client_id_2}])
    }

    brand_contract_id, contract_external_id = steps.ContractSteps.create_contract_new(ContractCommissionType.BRAND,
                                                                                      contract_params)
    print steps.CommonSteps.build_notification(10, object_id=client_id_1)

    def get_overdraft_object_id(firm_id, service_id, client_id):
        # To long int to use it in xmlrpc requests.
        return str(firm_id * 10 + service_id * 100000 + client_id * 1000000000)

    object_id = get_overdraft_object_id(Firms.YANDEX_1.id, service_id, client_id_2)

    another_client = steps.ClientSteps.create()
    collateral_params = {'CONTRACT2_ID': brand_contract_id}
    collateral_params.update({'DT': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt + relativedelta(days=3))),
                              'IS_SIGNED': Date.TODAY_ISO,
                              'BRAND_CLIENTS': json.dumps([{"id": "1", "num": client_id_1, "client": client_id_1},
                                                           {"id": "2", "num": client_id_2, "client": client_id_2},
                                                           {"id": "3", "num": another_client, "client": another_client},
                                                           ])})

    steps.ContractSteps.create_collateral(Collateral.BRAND_CHANGE, collateral_params)
    steps.ContractSteps.create_collateral(Collateral.BRAND_CHANGE, collateral_params)

    utils.check_that(lambda: steps.CommonSteps.get_last_notification(10, another_client),
                     matches_in_time(has_entries_casted(ClientNotification({
                         'HasEquivalentOrBrandClients': '1'
                     }).values), timeout=300))

    another_client2 = steps.ClientSteps.create()
    collateral_params = {'CONTRACT2_ID': brand_contract_id}
    collateral_params.update({'DT': utils.Date.to_iso(utils.Date.nullify_time_of_date(dt + relativedelta(days=3))),
                              'IS_SIGNED': Date.TODAY_ISO,
                              'BRAND_CLIENTS': json.dumps(
                                  [{"id": "1", "num": another_client2, "client": another_client2},
                                   ])})

    steps.ContractSteps.create_collateral(Collateral.BRAND_CHANGE, collateral_params)
    utils.check_that(lambda: steps.CommonSteps.get_last_notification(10, another_client),
                     matches_in_time(has_entries_casted(ClientNotification({
                         'HasEquivalentOrBrandClients': '0'
                     }).values), timeout=300))

    # 6
    # Можно будет проверить только на тм

    # GetRequestChoices


def test_new_get_request_choices():
    client_limit = 10000

    client_id, person_id, service_id, product_id, paysys_id, service_id, service_order_id, request_id, \
    invoice_id, order_id = prepare_client(limit=client_limit)

    _ = api.medium().GetRequestChoices(
        {'OperatorUid': defaults.PASSPORT_UID, 'RequestID': -1},
        client_id,
        [{'Qty': 600, 'ServiceID': 7, 'ServiceOrderID': service_order_id}],
        {}, )


@pytest.mark.parametrize('multicurrency', [True, False])
@pytest.mark.parametrize('days_after', [0, 5, 13, 20])
@pytest.mark.parametrize('client_limit', [900, 150])
def test_overdraft_get_request_choices(multicurrency, days_after, client_limit):
    client_id = steps.ClientSteps.create()

    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id
    paysys_id = Paysyses.BANK_UR_RUB.id
    qty = 300
    dt = datetime.datetime.now() - relativedelta(days=days_after)

    if multicurrency:
        # Переходим на мультивалютность миграцией
        steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                              dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2))
        # Разрешаем овердрафт
        steps.OverdraftSteps.set_force_overdraft(client_id, service_id, client_limit, Firms.YANDEX_1.id, currency='RUB')
    else:
        # Разрешаем овердрафт
        client_limit /= 30
        qty /= 30
        steps.OverdraftSteps.set_force_overdraft(client_id, service_id, client_limit, Firms.YANDEX_1.id)

    person_id = steps.PersonSteps.create(client_id, 'ur')

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product_id, params={'AgencyID': None})
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': qty}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=dt))

    overdraft_params = api.medium().GetRequestChoices({'OperatorUid': Users.YB_ADM.uid,
                                                       'RequestID': request_id,
                                                       'PersonID': person_id})['overdrafts']

    # доступен ли овердрафт
    assert overdraft_params.get('is_present') is True
    # подходит ли счет для овердрафта
    assert overdraft_params.get('is_available') is (True if client_limit > qty else False)
    assert overdraft_params.get('currency') == 'RUR'
    assert overdraft_params.get('iso_currency') == 'RUB'
    if multicurrency:
        assert overdraft_params.get('overdraft_iso_currency') == 'RUB'
    else:
        assert overdraft_params.get('overdraft_iso_currency') is None
    # деньги
    assert overdraft_params.get('available_sum') == str(client_limit if multicurrency else client_limit * 30)
    # фишки, но для валютных - деньги
    assert overdraft_params.get('available_sum_ue') == str(client_limit)
    # до срока 5 дней
    assert overdraft_params.get('nearly_expired_sum') == '0'
    assert overdraft_params.get('nearly_expired_sum_ue') == '0'
    # протухшее
    assert overdraft_params.get('expired_sum') == '0'
    assert overdraft_params.get('expired_sum_ue') == '0'
    # все зачисления
    assert overdraft_params.get('spent_sum') == '0'
    assert overdraft_params.get('spent_sum_ue') == '0'
    assert overdraft_params.get('min_days_to_live') is None

    try:
        invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0, contract_id=None,
                                                     overdraft=1, endbuyer_id=None)
    except Exception, exc:
        utils.check_that(steps.CommonSteps.get_exception_code(exc), equal_to('NOT_ENOUGH_OVERDRAFT_LIMIT'))
        if qty <= client_limit:
            assert 1 == 2

    if qty <= client_limit and days_after < 15:
        service_order_id_2 = steps.OrderSteps.next_id(service_id=service_id)
        order_id_2 = steps.OrderSteps.create(client_id, service_order_id_2, service_id=service_id,
                                             product_id=product_id, params={'AgencyID': None})
        orders_list_2 = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id_2, 'Qty': qty}]
        request_id = steps.RequestSteps.create(client_id, orders_list_2, additional_params=dict(InvoiceDesireDT=dt))
        overdraft_params = api.medium().GetRequestChoices({'OperatorUid': Users.YB_ADM.uid,
                                                           'RequestID': request_id,
                                                           'PersonID': person_id})['overdrafts']

        assert overdraft_params.get('is_present') is True
        assert overdraft_params.get('is_available') is (True if client_limit > qty else False)
        assert overdraft_params.get('currency') == 'RUR'
        assert overdraft_params.get('iso_currency') == 'RUB'
        if multicurrency:
            assert overdraft_params.get('overdraft_iso_currency') == 'RUB'
        else:
            assert overdraft_params.get('overdraft_iso_currency') is None
        assert overdraft_params.get('available_sum') == str(
            (client_limit - qty) if multicurrency else (client_limit - qty) * 30)
        assert overdraft_params.get('available_sum_ue') == str(client_limit - qty)
        assert overdraft_params.get('nearly_expired_sum') == str(
            (qty if multicurrency else qty * 30) if days_after == 13 else 0)
        assert overdraft_params.get('nearly_expired_sum_ue') == str(qty if days_after == 13 else 0)
        assert overdraft_params.get('expired_sum') == str(
            (qty if multicurrency else qty * 30) if days_after == 20 else 0)
        assert overdraft_params.get('expired_sum_ue') == str(qty if days_after == 20 else 0)
        assert overdraft_params.get('spent_sum') == str(qty if multicurrency else qty * 30)
        assert overdraft_params.get('spent_sum_ue') == str(qty)
        if 10 < days_after < 15:
            assert overdraft_params.get('min_days_to_live') == 15 - days_after
        else:
            assert overdraft_params.get('min_days_to_live') is None


# Дополнительно
# Перекрутки сверх лимита в счет не попадают и отбрасываются пропорционально открученному по каждому заказу.
# Тут обязательно нужно посмотреть как это работает с разнородными заказами, в частности с фишечными и денежными.
def test_fish_overshipment():
    limit = 90
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id
    paysys_id = Paysyses.BANK_UR_RUB.id
    # now = datetime.datetime.now()
    now = LAST_DAY_OF_PREVIOUS_MONTH

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product_id, params={'AgencyID': None})

    # Кладем на заказы равное количество денег
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 20, 'BeginDT': now}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
                                                 contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    # Перекрут по заказу
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Bucks': 25}, 0, now - relativedelta(days=1))

    # Переходим на мультивалютность миграцией
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2))

    # Разрешаем овердрафт
    steps.OverdraftSteps.set_force_overdraft(client_id, service_id, limit, Firms.YANDEX_1.id, currency='RUB')

    # person_id = steps.PersonSteps.create(client_id, 'ur')

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)

    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product_id, params={'AgencyID': None})

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 600}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
                                                 contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    # Откурчиваем все по каждому заказу
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Money': 600}, 0, now - relativedelta(days=1))

    act_id = steps.ActsSteps.generate(client_id, force=1, date=now)[0]

    # Разрешаем автоовердрафт
    set_overdraft_params(person_id=person_id, client_limit=limit)

    # now = datetime.datetime.now()
    now = LAST_DAY_OF_PREVIOUS_MONTH
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Money': 900}, 0, now)

    now = datetime.datetime.now()
    service_order_id = steps.OrderSteps.next_id(service_id=service_id)

    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product_id, params={'AgencyID': None})

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 600}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
                                                 contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    # Перекручиваем
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Money': 630}, 0, now - relativedelta(days=1))

    id = db.balance().execute("SELECT ID FROM bo.t_overdraft_params WHERE client_id =:item",
                              {'item': client_id})[0]['id']

    steps.CommonSteps.export('AUTO_OVERDRAFT', 'OverdraftParams', id, with_enqueue=True)


def test_nedoshipment():
    limit = 90
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id
    paysys_id = Paysyses.BANK_UR_RUB.id
    # now = datetime.datetime.now()
    now = LAST_DAY_OF_PREVIOUS_MONTH

    # Переходим на мультивалютность миграцией
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2))

    # Разрешаем овердрафт
    steps.OverdraftSteps.set_force_overdraft(client_id, service_id, limit, Firms.YANDEX_1.id, currency='RUB')

    # person_id = steps.PersonSteps.create(client_id, 'ur')

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)

    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product_id, params={'AgencyID': None})

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 600}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
                                                 contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    # Откурчиваем все по каждому заказу
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Money': 400}, 0, now - relativedelta(days=1))

    act_id = steps.ActsSteps.generate(client_id, force=1, date=now)[0]

    # Разрешаем автоовердрафт
    set_overdraft_params(person_id=person_id, client_limit=limit)

    # now = datetime.datetime.now()
    now = LAST_DAY_OF_PREVIOUS_MONTH
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Money': 900}, 0, now)

    id = db.balance().execute("SELECT ID FROM bo.t_overdraft_params WHERE client_id =:item",
                              {'item': client_id})[0]['id']

    steps.CommonSteps.export('AUTO_OVERDRAFT', 'OverdraftParams', id, with_enqueue=True)


def test_overshipment():
    limit = 90
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id
    paysys_id = Paysyses.BANK_UR_RUB.id
    # now = datetime.datetime.now()
    now = LAST_DAY_OF_PREVIOUS_MONTH

    # Переходим на мультивалютность миграцией
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2))

    # Разрешаем овердрафт
    steps.OverdraftSteps.set_force_overdraft(client_id, service_id, limit, Firms.YANDEX_1.id, currency='RUB')

    # person_id = steps.PersonSteps.create(client_id, 'ur')

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)

    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product_id, params={'AgencyID': None})

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 600}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
                                                 contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    # Откурчиваем все по каждому заказу
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Money': 900}, 0, now - relativedelta(days=1))

    act_id = steps.ActsSteps.generate(client_id, force=1, date=now)[0]

    # Разрешаем автоовердрафт
    set_overdraft_params(person_id=person_id, client_limit=limit)

    id = db.balance().execute("SELECT ID FROM bo.t_overdraft_params WHERE client_id =:item",
                              {'item': client_id})[0]['id']

    steps.CommonSteps.export('AUTO_OVERDRAFT', 'OverdraftParams', id, with_enqueue=True)


def test_svobodnie_sredstva():
    limit = 90
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id
    paysys_id = Paysyses.BANK_UR_RUB.id
    # now = datetime.datetime.now()
    now = LAST_DAY_OF_PREVIOUS_MONTH

    # Переходим на мультивалютность миграцией
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2))

    # Разрешаем овердрафт
    steps.OverdraftSteps.set_force_overdraft(client_id, service_id, limit, Firms.YANDEX_1.id, currency='RUB')

    # person_id = steps.PersonSteps.create(client_id, 'ur')

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)

    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product_id, params={'AgencyID': None})

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 600}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
                                                 contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    # Откурчиваем все по каждому заказу
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Money': 400}, 0, now - relativedelta(days=1))

    act_id = steps.ActsSteps.generate(client_id, force=1, date=now)[0]

    # Разрешаем автоовердрафт
    set_overdraft_params(person_id=person_id, client_limit=limit)

    # now = datetime.datetime.now()
    now = LAST_DAY_OF_PREVIOUS_MONTH
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Money': 900}, 0, now)

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)

    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product_id, params={'AgencyID': None})

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 600}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
                                                 contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    # Откурчиваем все по каждому заказу
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Money': 300}, 0, now - relativedelta(days=1))

    id = db.balance().execute("SELECT ID FROM bo.t_overdraft_params WHERE client_id =:item",
                              {'item': client_id})[0]['id']

    steps.CommonSteps.export('AUTO_OVERDRAFT', 'OverdraftParams', id, with_enqueue=True)


def test_dva_servisa():
    limit = 90
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id
    paysys_id = Paysyses.BANK_UR_RUB.id
    # now = datetime.datetime.now()
    now = LAST_DAY_OF_PREVIOUS_MONTH

    # Переходим на мультивалютность миграцией
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2))

    # Разрешаем овердрафт
    steps.OverdraftSteps.set_force_overdraft(client_id, service_id, limit, Firms.YANDEX_1.id, currency='RUB')

    # person_id = steps.PersonSteps.create(client_id, 'ur')

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)

    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product_id, params={'AgencyID': None})

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 600}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
                                                 contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    # Откурчиваем все по каждому заказу
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Money': 400}, 0, now - relativedelta(days=1))

    act_id = steps.ActsSteps.generate(client_id, force=1, date=now)[0]

    # Разрешаем автоовердрафт
    set_overdraft_params(person_id=person_id, client_limit=limit)

    # now = datetime.datetime.now()
    now = LAST_DAY_OF_PREVIOUS_MONTH
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Money': 900}, 0, now)

    service_order_id = steps.OrderSteps.next_id(service_id=Services.MARKET.id)

    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=Services.MARKET.id,
                                       product_id=Products.MARKET.id, params={'AgencyID': None})

    orders_list = [{'ServiceID': Services.MARKET.id, 'ServiceOrderID': service_order_id, 'Qty': 20}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
                                                 contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    # Откурчиваем все по каждому заказу
    steps.CampaignsSteps.do_campaigns(Services.MARKET.id, service_order_id, {'Money': 900}, 0,
                                      now - relativedelta(days=1))

    act_id = steps.ActsSteps.generate(client_id, force=1, date=now)[0]
    ######################

    id = db.balance().execute("SELECT ID FROM bo.t_overdraft_params WHERE client_id =:item",
                              {'item': client_id})[0]['id']

    steps.CommonSteps.export('AUTO_OVERDRAFT', 'OverdraftParams', id, with_enqueue=True)


def test_base_test():
    limit = 90
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')

    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id
    paysys_id = Paysyses.BANK_UR_RUB.id
    # now = datetime.datetime.now()
    now = LAST_DAY_OF_PREVIOUS_MONTH

    # Переходим на мультивалютность миграцией
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2))

    # Разрешаем овердрафт
    steps.OverdraftSteps.set_force_overdraft(client_id, service_id, limit, Firms.YANDEX_1.id, currency='RUB')
    # Разрешаем автоовердрафт
    set_overdraft_params(person_id=person_id, client_limit=limit)

    # person_id = steps.PersonSteps.create(client_id, 'ur')

    service_order_id = steps.OrderSteps.next_id(service_id=service_id)

    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product_id, params={'AgencyID': None})

    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 600}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
    invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
                                                 contract_id=None, overdraft=0, endbuyer_id=None)
    steps.InvoiceSteps.pay(invoice_id)

    now = LAST_DAY_OF_PREVIOUS_MONTH
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Money': 630}, 0, now)

    now = datetime.datetime.now()
    steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Money': 660}, 0, now)

    act_id = steps.ActsSteps.generate(client_id, force=1, date=now)[0]

    id = db.balance().execute("SELECT ID FROM bo.t_overdraft_params WHERE client_id =:item",
                              {'item': client_id})[0]['id']

    steps.CommonSteps.export('AUTO_OVERDRAFT', 'OverdraftParams', id, with_enqueue=True)


# Нельзя овердрафт при включенном автоовердрафте
def test_overdraft_after_autooverdraft():
    client_limit = 10000
    client_id = steps.ClientSteps.create()
    # Переходим на мультивалютность миграцией
    steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
                                          dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2))

    service_id = Services.DIRECT.id
    product_id = Products.DIRECT_FISH.id
    paysys_id = Paysyses.BANK_UR_RUB.id

    # now = datetime.datetime.now()
    now = LAST_DAY_OF_PREVIOUS_MONTH

    # Разрешаем овердрафт
    steps.OverdraftSteps.set_force_overdraft(client_id, service_id, client_limit, Firms.YANDEX_1.id, currency='RUB')

    person_id = steps.PersonSteps.create(client_id, 'ur')

    # Создаем заказы с единым счетом
    service_order_id = steps.OrderSteps.next_id(service_id=service_id)
    order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
                                       product_id=product_id, params={'AgencyID': None})

    # Кладем на заказы равное количество денег
    orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 1500}]
    request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))

    set_overdraft_params(person_id=person_id, client_limit=client_limit)

    set_overdraft_params(person_id=person_id, client_limit=0)

####################################
# НЕ НУЖНЫ, НО УДАЛЯТЬ ПОКА НЕ БУДУ


# def test_big_limit():  # Это проверяет Директ
#     client_limit = 300
#
#     client_id, person_id, service_id, product_id, paysys_id, service_id, service_order_id, request_id, \
#     invoice_id, order_id = prepare_client(limit=client_limit)
#
#     # Разрешаем автоовердрафт
#     set_overdraft_params(person_id=person_id, client_limit=900)
#
#     now = LAST_DAY_OF_PREVIOUS_MONTH
#     steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Money': 1800}, 0, now)
#
#     id = db.balance().execute("SELECT ID FROM bo.t_overdraft_params WHERE client_id =:item",
#                               {'item': client_id})[0]['id']
#
#     steps.CommonSteps.export('AUTO_OVERDRAFT', 'OverdraftParams', id, with_enqueue=True)
#
#     client_limit = db.balance().execute("SELECT CLIENT_LIMIT FROM bo.t_overdraft_params WHERE client_id =:item",
#                                         {'item': client_id})[0]['client_limit']
#
#     print client_limit
#     # Какой лимит то в итоге???


# def test_another_currency():
#     client_limit = 10000
#
#     client_id, person_id, service_id, product_id, paysys_id, service_id, service_order_id, request_id, \
#       invoice_id, order_id = prepare_client(limit=client_limit)
#
#     iso_currency = 'SAR'
#
#     try:
#         # Разрешаем автоовердрафт
#         set_overdraft_params(person_id=person_id, client_limit=client_limit, iso_currency=iso_currency)
#     except Exception, exc:
#         utils.check_that(steps.CommonSteps.get_exception_code(exc), equal_to('INVALID_PARAM'))
#         utils.check_that(steps.CommonSteps.get_exception_code(exc, tag_name='msg'),
#                          equal_to('Invalid parameter for function: Invalid iso_currency={}'.format(iso_currency)))
#     else:
#         assert 1 == 2


# def test_otkl_auto(): # Директ не должен так делать, он обещал
#     limit = 90
#     client_id = steps.ClientSteps.create()
#     person_id = steps.PersonSteps.create(client_id, 'ur')
#
#     service_id = Services.DIRECT.id
#     product_id = Products.DIRECT_FISH.id
#     paysys_id = Paysyses.BANK_UR_RUB.id
#     # now = datetime.datetime.now()
#     now = LAST_DAY_OF_PREVIOUS_MONTH
#
#     # Переходим на мультивалютность миграцией
#     steps.ClientSteps.migrate_to_currency(client_id, currency_convert_type='MODIFY',
#                                           dt=LAST_DAY_OF_PREVIOUS_MONTH - relativedelta(days=2))
#
#     # Разрешаем овердрафт
#     steps.OverdraftSteps.set_force_overdraft(client_id, service_id, limit, Firms.YANDEX_1.id, currency='RUB')
#
#     # person_id = steps.PersonSteps.create(client_id, 'ur')
#     service_order_id = steps.OrderSteps.next_id(service_id=service_id)
#
#     order_id = steps.OrderSteps.create(client_id, service_order_id, service_id=service_id,
#                                        product_id=product_id, params={'AgencyID': None})
#
#     orders_list = [{'ServiceID': service_id, 'ServiceOrderID': service_order_id, 'Qty': 600}]
#     request_id = steps.RequestSteps.create(client_id, orders_list, additional_params=dict(InvoiceDesireDT=now))
#     invoice_id, _, _ = steps.InvoiceSteps.create(request_id, person_id, paysys_id, credit=0,
#                                                  contract_id=None, overdraft=0, endbuyer_id=None)
#     steps.InvoiceSteps.pay(invoice_id)
#
#     # Откурчиваем все по каждому заказу
#     steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Money': 400}, 0, now - relativedelta(days=1))
#
#     act_id = steps.ActsSteps.generate(client_id, force=1, date=now)[0]
#     # Разрешаем автоовердрафт
#     set_overdraft_params(person_id=person_id, client_limit=limit)
#
#     # now = datetime.datetime.now()
#     now = LAST_DAY_OF_PREVIOUS_MONTH
#     steps.CampaignsSteps.do_campaigns(service_id, service_order_id, {'Money': 630}, 0, now)
#
#     set_overdraft_params(person_id=person_id, client_limit=0)
#
#     id = db.balance().execute("SELECT ID FROM bo.t_overdraft_params WHERE client_id =:item",
#                               {'item': client_id})[0]['id']
#
#     steps.CommonSteps.export('AUTO_OVERDRAFT', 'OverdraftParams', id, with_enqueue=True)
