# -*- coding: utf-8 -*-

from datetime import datetime
from decimal import Decimal
import time
import uuid

import pytest
from hamcrest import is_in, empty, contains_string, equal_to

from balance import balance_steps as steps

from btestlib import utils, reporter
from btestlib.constants import Services
from simpleapi.common.payment_methods import  Cash
from check.shared import CheckSharedBefore
from check import db, shared_steps, utils as ch_utils
from btestlib import utils as butils
from btestlib.data import defaults
from simpleapi.data.uids_pool import User
from btestlib.data.partner_contexts import CORP_TAXI_RU_CONTEXT_SPENDABLE, CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED, CORP_TAXI_RU_CONTEXT_SPENDABLE_MIGRATED
from btestlib import environments as env


"""
Сверка Яндекс.Корпоративное Такси (платежи)

Забор данных

Забираем данные из биллинга
sql запрос(платежи):
query = '''
        select {hints_comment}
      t.trust_payment_id as id,
      -- MSK -> UTC
      t.postauth_dt - interval '3' hour as dt,
      sp.partner_id,
      decode(p.currency, 'RUR', 'RUB', p.currency) as currency,
      p.amount as amount
    from bo.t_payment p
    join bo.t_ccard_bound_payment t on t.id = p.id
    join bo.t_request_order ro on ro.request_id = t.request_id
    join bo.t_order o on o.id = ro.parent_order_id
    join bo.t_service_product sp on sp.id = o.service_product_id
    -- Дополнительные условия по service_id ускоряют запрос 
    -- в случае непустого objects_filter
    where p.service_id = {Services.taxi_corp.id:d}
      and o.service_id = {Services.taxi_corp.id:d}
      and sp.service_id = {Services.taxi_corp.id:d}
      -- BALANCE-24327
      and t.postauth_dt is not null
      -- UTC -> MSK
      and t.postauth_dt >= {period_begin_dt} + interval '3' hour
      and t.postauth_dt < {period_end_dt} + 1 + interval '3' hour
      {compensations_filter}
      {objects_filter}
        '''
sql запрос(возвраты):
query = '''
        select {hints_comment}
      r.trust_refund_id as id,
      -- MSK -> UTC
      p.payment_dt - interval '3' hour as dt,
      sp.partner_id,
      decode(p.currency, 'RUR', 'RUB', p.currency) as currency,
      p.amount
    /* t_payment - это базовая таблица для всех видов платежей, включая refund,
       т.е. refund - это subclass payment. Поэтому payment здесь нужно
       воспринимать, как часть refund, а не как платеж, который вернули этим
       refund'ом. */
    from bo.t_payment p
    join bo.t_refund r on r.id = p.id
    join bo.t_ccard_bound_payment t on t.id = r.orig_payment_id
    join bo.t_request_order ro on ro.request_id = t.request_id
    join bo.t_order o on o.id = ro.parent_order_id
    join bo.t_service_product sp on sp.id = o.service_product_id
    where p.paysys_code = 'REFUND'
      and p.service_id = {Services.taxi_corp.id:d}
      and o.service_id = {Services.taxi_corp.id:d}
      and sp.service_id = {Services.taxi_corp.id:d}
      -- UTC -> MSK
      and p.payment_dt >= {period_begin_dt} + interval '3' hour
      and p.payment_dt < {period_end_dt} + 1 + interval '3' hour
      {compensations_filter}
      {objects_filter}
        '''

Забираем данные из YT
поля из таблицы
yield self.destination_table.serialize_row({
            'id': row['payment_id'],
            'type': row['type'],
            'partner_id': partner_id,
            'currency': row['currency'],
            'amount': Decimal(row['value']),
            'dt': datetime.utcfromtimestamp(row['postauth_dt']),
            'system': Names.taxi
        })
   

Типы расхождений:
* 1 - Отсутствует в такси
* 2 - Отсутствует в биллинге
* 3 - Расходится ID партнера
* 4 - Расходится валюта
* 5 - Расходится сумма
* 10 - В такси обнаружено несколько записей по уникальному ключу
* 11 - В биллинге обнаружено несколько записей по уникальному ключу
"""

SERVICE = Services.TAXI_CORP
PAYMETHOD = Cash()
contract_start_dt = datetime.today().replace(day=1)

PAYMENT_PRICE = Decimal('3608.01')
PAYMENT_SERVICE_PRICE = Decimal('792')
REFUND_PRICE = Decimal('1189.01')
REFUND_SERVICE_PRICE = Decimal('261')

def create_clients_persons_contracts(user):
    taxi_client_id, service_product_id = steps.SimpleApi.create_partner_and_product(
        CORP_TAXI_RU_CONTEXT_SPENDABLE_MIGRATED.service)
    _, taxi_person_id, taxi_contract_id, _ = steps.ContractSteps. \
        create_partner_contract(CORP_TAXI_RU_CONTEXT_SPENDABLE_MIGRATED, client_id=taxi_client_id,
                                additional_params={'start_dt': contract_start_dt})

    corp_client_id, corp_person_id, corp_contract_id, _ = steps.ContractSteps. \
        create_partner_contract(CORP_TAXI_RU_CONTEXT_GENERAL_MIGRATED,
                                additional_params={'start_dt': contract_start_dt})

    # привязываем логин к корпоративному клиенту
    steps.UserSteps.link_user_and_client(user, corp_client_id)

    return service_product_id, corp_client_id, taxi_contract_id, \
           taxi_client_id, taxi_person_id


def create_client_with_user():
    user = User(defaults.taxi_corp()['UID_TO_LINK'], defaults.taxi_corp()['LOGIN_TO_LINK'], None)
    service_product_id, corp_client_id, taxi_contract_spendable_id, \
    taxi_client_id, taxi_person_partner_id = create_clients_persons_contracts(user)
    return user, service_product_id, corp_client_id, taxi_contract_spendable_id, taxi_client_id, taxi_person_partner_id


def get_create_clients_persons_contracts_list(check_count=11):
    result = []
    for i in range(check_count):
        result.append(create_client_with_user())
    return result


def get_client_data(data_list):
    client_data = data_list.pop()
    return client_data


def create_payments(user, service_product_id, corp_client_id, taxi_contract_spendable_id, taxi_client_id,
                    taxi_person_partner_id):
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(SERVICE,
                                             service_product_id,
                                             paymethod=PAYMETHOD,
                                             user=user,
                                             order_dt=utils.Date.moscow_offset_dt())

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)
    payment_data = db.get_payment_data(payment_id)[0]
    print payment_data['trust_payment_id']
    return payment_data, taxi_client_id


def create_multiple_payments(context_spendable, service_product_ids, user):
    service_order_id_list, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_multiple_trust_payments(context_spendable.service,
                                                       service_product_ids,
                                                       prices_list=[PAYMENT_PRICE, PAYMENT_SERVICE_PRICE],
                                                       paymethod=PAYMETHOD,
                                                       currency=context_spendable.payment_currency,
                                                       user=user,
                                                       order_dt=utils.Date.moscow_offset_dt())

    # запускаем обработку платежа
    steps.CommonPartnerSteps.export_payment(payment_id)

    return service_order_id_list, trust_payment_id, purchase_token, payment_id


def create_multiple_refunds(context_spendable, service_order_id_list, trust_payment_id):
    trust_refund_id, refund_id = steps.SimpleApi.create_multiple_refunds(context_spendable.service,
                                                                         service_order_id_list,
                                                                         trust_payment_id,
                                                                         delta_amount_list=[REFUND_PRICE,
                                                                                            REFUND_SERVICE_PRICE])

    # запускаем обработку рефанда
    steps.CommonPartnerSteps.export_payment(refund_id)

    return trust_refund_id, refund_id

@pytest.fixture(scope='module')
def fixtures():
    # Этот этап нужен только для noshared или before
    shared_type = pytest.config.getoption('shared')
    if shared_type and shared_type != 'before':
        return

    # Enable service's params
    env.SimpleapiEnvironment.switch_param(**utils.remove_empty(dict(service=SERVICE, dbname=None,
                                                                    xmlrpc_url=None)))

    data_list = get_create_clients_persons_contracts_list()
    create_clients_persons_contracts_ = lambda: get_client_data(data_list)

    # env.SimpleapiEnvironment.switch_param()

    return create_clients_persons_contracts_


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CPBT)
def test_without_diff(shared_data, fixtures):
    """
    Начальные условия:
        -платеж с присутствует и в yt, и в биллинге
        -Все сверяемые поля сходятся
    Ожидаемый результат:
        платеж отсутствует в списке расхождений
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['partner_id', 'yt_data']) as before:
        before.validate()

        clients_persons_contracts = fixtures

        payment_data, partner_id = create_payments(*clients_persons_contracts())

        yt_data = [{
            "client_id": str(partner_id),
            "currency": "RUB",
            "payment_id": payment_data['trust_payment_id'],
            "type": "payment",
            "postauth_dt": time.mktime(payment_data['payment_dt'].timetuple()),
            "value": payment_data['amount']
        }]



    cmp_data = shared_steps.SharedBlocks.run_cpbt(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    # cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" %cmp_data)

    result = [(row['billing_partner_id'], row['state'])
              for row in cmp_data if row['billing_partner_id'] == int(partner_id)]
    reporter.log("Result = %s" % result)

    butils.check_that(result, empty())


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CPBT)
def test_not_found_in_taxi(shared_data, fixtures):
    """
    Начальные условия:
        -платеж присутствует в биллинге
        -платеж отсутствует в yt
    Ожидаемый результат:
        попадает в список с расхождений,
        состояние = "Отсутствует в такси"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['partner_id', 'yt_data']) as before:
        before.validate()

        clients_persons_contracts = fixtures

        payment_data, partner_id = create_payments(*clients_persons_contracts())

        yt_data = None

    cmp_data = shared_steps.SharedBlocks.run_cpbt(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" %cmp_data)

    result = [(row['billing_partner_id'], row['state'])
              for row in cmp_data if row['billing_partner_id'] == int(partner_id)]
    reporter.log("Result = %s" % result)

    butils.check_that((int(partner_id), 1), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CPBT)
def test_not_found_in_bill(shared_data, fixtures):
    """
    Начальные условия:
        -платеж отсутствует в биллинге
        -платеж присутствует в yt
    Ожидаемый результат:
        попадает в список с расхождений,
        состояние = "Отсутствует в биллинге"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['partner_id', 'yt_data']) as before:
        before.validate()

        clients_persons_contracts = fixtures

        trust_payment_id = str(uuid.uuid4()).replace('-', '')[0:24]

        _, _, _, _, taxi_client_id_, _ = clients_persons_contracts()

        payment_data, _ = create_payments(*clients_persons_contracts())

        partner_id = taxi_client_id_

        yt_data = [{
            "client_id": str(partner_id),
            "currency": "RUB",
            "payment_id": trust_payment_id,
            "type": "payment",
            "postauth_dt": time.mktime(payment_data['payment_dt'].timetuple()),
            "value": payment_data['amount']
        }]



    cmp_data = shared_steps.SharedBlocks.run_cpbt(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" %cmp_data)

    result = [(row['taxi_partner_id'], row['state'])
              for row in cmp_data if row['taxi_partner_id'] == int(partner_id)]
    reporter.log("Result = %s" % result)

    butils.check_that((int(partner_id), 2), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CPBT)
def test_partner_id_not_converge(shared_data, fixtures):
    """
    Начальные условия:
        -платеж присутствует в биллинге, partner_id = 7655681
        -платеж присутствует в yt, partner_id = 556272
    Ожидаемый результат:
        попадает в список с расхождений,
        состояние = "Расходится ID партнера"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['partner_id', 'taxi_client_id_', 'yt_data']) as before:
        before.validate()

        clients_persons_contracts = fixtures

        payment_data, partner_id = create_payments(*clients_persons_contracts())
        _, _, _, _, taxi_client_id_, _ = clients_persons_contracts()

        yt_data = [{
            "client_id": str(taxi_client_id_),
            "currency": "RUB",
            "payment_id": payment_data['trust_payment_id'],
            "type": "payment",
            "postauth_dt": time.mktime(payment_data['payment_dt'].timetuple()),
            "value": payment_data['amount']
        }]


    cmp_data = shared_steps.SharedBlocks.run_cpbt(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" %cmp_data)

    result = [(row['billing_partner_id'], row['state'])
              for row in cmp_data if row['billing_partner_id'] == int(partner_id)]
    reporter.log("Result = %s" % result)

    butils.check_that((int(partner_id), 3), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CPBT)
def test_currency_not_converge(shared_data, fixtures):
    """
    Начальные условия:
        -платеж присутствует присутствует в биллинге, валюта = 'rub'
        -платеж присутствует присутствует в yt, валюта = 'usd'
    Ожидаемый результат:
        попадает в список с расхождений,
        состояние = "Расходится валюта"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['partner_id', 'yt_data']) as before:
        before.validate()

        clients_persons_contracts = fixtures

        payment_data, partner_id = create_payments(*clients_persons_contracts())

        yt_data = [{
            "client_id": str(partner_id),
            "currency": "USD",
            "payment_id": payment_data['trust_payment_id'],
            "type": "payment",
            "postauth_dt": time.mktime(payment_data['payment_dt'].timetuple()),
            "value": payment_data['amount']
        }]

    cmp_data = shared_steps.SharedBlocks.run_cpbt(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" %cmp_data)

    result = [(row['billing_partner_id'], row['state'])
              for row in cmp_data if row['billing_partner_id'] == int(partner_id)]
    reporter.log("Result = %s" % result)

    butils.check_that((int(partner_id), 4), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CPBT)
def test_sum_not_converge(shared_data, fixtures):
    """
    Начальные условия:
        -платеж присутствует в биллинге, сумма = 619
        -платеж присутствует в yt, сумма = 111
    Ожидаемый результат:
        попадает в список с расхождений,
        состояние = "Расходится сумма"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['partner_id', 'yt_data']) as before:
        before.validate()

        clients_persons_contracts = fixtures

        payment_data, partner_id = create_payments(*clients_persons_contracts())

        yt_data = [{
            "client_id": str(partner_id),
            "currency": "RUB",
            "payment_id": payment_data['trust_payment_id'],
            "type": "payment",
            "postauth_dt": time.mktime(payment_data['payment_dt'].timetuple()),
            "value": str(Decimal(payment_data['amount']) + 20)
        }]

    cmp_data = shared_steps.SharedBlocks.run_cpbt(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" %cmp_data)

    result = [(row['billing_partner_id'], row['state'])
              for row in cmp_data if row['billing_partner_id'] == int(partner_id)]
    reporter.log("Result = %s" % result)

    butils.check_that((int(partner_id), 5), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CPBT)
def test_two_row_in_yt(shared_data, fixtures):
    """
    Начальные условия:
        -платеж присутствует в биллинге
        -платеж присутствует в yt, но представлен двумя одинаковыми строками
    Ожидаемый результат:
        попадает в список с расхождений,
        состояние = "В такси обнаружено несколько записей по уникальному ключу"
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['partner_id', 'yt_data']) as before:
        before.validate()

        clients_persons_contracts = fixtures

        payment_data, partner_id = create_payments(*clients_persons_contracts())

        base = {
            "client_id": str(partner_id),
            "currency": "RUB",
            "payment_id": payment_data['trust_payment_id'],
            "type": "payment",
            "postauth_dt": time.mktime(payment_data['payment_dt'].timetuple()),
            "value": payment_data['amount']
        }

        first_data = dict(base)  # Создаём копию словаря 'base'
        second_data = dict(base)

        yt_data = [first_data, second_data]

    cmp_data = shared_steps.SharedBlocks.run_cpbt(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" %cmp_data)

    result = [(row['billing_partner_id'], row['state'])
              for row in cmp_data if row['billing_partner_id'] == int(partner_id)]
    reporter.log("Result = %s" % result)

    butils.check_that((int(partner_id), 10), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CPBT)
def test_skip_amd_currency(shared_data, fixtures):
    """
    Начальные условия:
        -платеж присутствует присутствует в yt, валюта = 'amd'
        -платеж присутствует отсутствует в биллинге
    Ожидаемый результат:
        платеж отсутствует в списке расхождений
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['partner_id', 'yt_data']) as before:
        before.validate()

        clients_persons_contracts = fixtures

        _, _, _, _, partner_id, _ = clients_persons_contracts()
        payment_data, _ = create_payments(*clients_persons_contracts())

        trust_payment_id = str(uuid.uuid4()).replace('-', '')[0:24]

        yt_data = [{
            "client_id": str(partner_id),
            "currency": "AMD",
            "payment_id": trust_payment_id,
            "type": "payment",
            "postauth_dt": time.mktime(payment_data['payment_dt'].timetuple()),
            "value": payment_data['amount']
        }]

    cmp_data = shared_steps.SharedBlocks.run_cpbt(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" %cmp_data)

    result = [(row['billing_partner_id'], row['state'])
              for row in cmp_data if row['billing_partner_id'] == int(partner_id)]
    reporter.log("Result = %s" % result)

    butils.check_that(result, empty())


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CPBT)
def test_check_3063_incorrect_data(shared_data, fixtures):
    """
    Начальные условия:
        -платеж с присутствует и в yt, и в биллинге
        -в поле value из сервиса получаем некорректные данные "None"
    Ожидаемый результат:
        платеж присутствует в списке расхождений
        Есть авторазбор
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['partner_id', 'yt_data']) as before:
        before.validate()

        clients_persons_contracts = fixtures

        payment_data, partner_id = create_payments(*clients_persons_contracts())

        yt_data = [{
            "client_id": str(partner_id),
            "currency": "RUB",
            "payment_id": payment_data['trust_payment_id'],
            "type": "payment",
            "postauth_dt": time.mktime(payment_data['payment_dt'].timetuple()),
            "value": None
        }]



    cmp_data = shared_steps.SharedBlocks.run_cpbt(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    # cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    cmp_id = cmp_data[0]['cmp_id']

    result = [(row['billing_partner_id'], row['state'])
              for row in cmp_data if row['billing_partner_id'] == int(partner_id)]
    reporter.log("CMP_DATA = %s" % cmp_data)
    reporter.log("Result = %s" % result)

    butils.check_that((int(partner_id), 5), is_in(result))

    ticket = ch_utils.get_check_ticket('cpbt', cmp_id)
    rationale = ch_utils.get_db_config_value('cpbt_incorrect_data_report_issue_comment')

    comments = list(ticket.comments.get_all())
    for comment in comments:
        if rationale in comment.text:
            butils.check_that(comment.text, contains_string(str(partner_id))
            )
            break
    else:
        assert False, u'Комментарий авторазбора не найден'


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CPBT)
def test_CHECK_3038_refund_with_service_fee_without_diff(shared_data, fixtures):
    """
    Начальные условия:
        -платеж присутствует и в yt, и в биллинге. Платёж состоит из 2-х частей(для продукта с service_fee и без)
        -возврат присутствует и в yt, и в биллинге. Возврат состоит из 2-х частей(для продукта с service_fee и без)
        - Для продукта с "service_fee" возврат "обрезается" по условию задачи
        -Все сверяемые поля сходятся
    Ожидаемый результат:
        платеж отсутствует в списке расхождений
    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['partner_id', 'yt_data']) as before:
        before.validate()

        context_spendable = CORP_TAXI_RU_CONTEXT_SPENDABLE_MIGRATED

        user = User(defaults.taxi_corp()['UID_TO_LINK'], defaults.taxi_corp()['LOGIN_TO_LINK'], None)

        service_product_id, corp_client_id, taxi_contract_spendable_id, \
        partner_id, taxi_person_partner_id = create_clients_persons_contracts(user)

        # создаем ещё один продукт но уже с service_fee=1
        service_product_id_fee = steps.SimpleApi.create_service_product(context_spendable.service,
                                                                        partner_id,
                                                                        service_fee=1)

        service_product_ids = [service_product_id, service_product_id_fee]

        # создаем платеж
        service_order_id_list, trust_payment_id, purchase_token, payment_id = \
            create_multiple_payments(context_spendable, service_product_ids, user)

        # Создаём refund
        trust_refund_id, refund_id = \
            create_multiple_refunds(context_spendable, service_order_id_list, trust_payment_id)

        payment_data = db.get_payment_data(payment_id)[0]
        refund_data = db.get_payment_data(refund_id)[0]

        # Подготавливаем данные по платежу и возврату для инсёрат в YT
        yt_data = [
            {
                "client_id": str(partner_id),
                "currency": "RUB",
                "payment_id": payment_data['trust_payment_id'],
                "type": "payment",
                "postauth_dt": time.mktime(payment_data['payment_dt'].timetuple()),
                "value": payment_data['amount']
            },
            {
                "client_id": str(partner_id),
                "currency": "RUB",
                "payment_id": trust_refund_id,
                "type": "refund",
                "postauth_dt": time.mktime(payment_data['payment_dt'].timetuple()),
                "value": str(REFUND_PRICE)
            }
        ]

    cmp_data = shared_steps.SharedBlocks.run_cpbt(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    # cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" %cmp_data)

    result = [(row['billing_partner_id'], row['state'])
              for row in cmp_data if row['billing_partner_id'] == int(partner_id)]
    reporter.log("Result = %s" % result)

    # Не должно быть расхождений, т.к. "лишний" 'refund' обрезается в запросе в самом коде сверки
    butils.check_that(result, empty())


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_CPBT)
def test_check_diffs_count(shared_data):
    """
        В тесте проверяем, что общее количество росхождений в тестах совпадает с ожидаемым
    """
    diffs_count = 7

    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_cpbt(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    butils.check_that(len(cmp_data), equal_to(diffs_count))
