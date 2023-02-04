# coding: utf-8
__author__ = 'chihiro'

import json
import uuid
from datetime import datetime, timedelta

from collections import namedtuple
import pytest
from hamcrest import is_in, empty, has_length, has_string

from balance import balance_db as db
from btestlib.constants import Services
from check import steps as check_steps
from btestlib import utils as butils
import btestlib.reporter as reporter
from check import shared_steps
from check.shared import CheckSharedBefore


def create_sidepayment(price, dt, client_id, currency, transaction_dt=None):
    transaction_id = uuid.uuid4()
    payload = {
        'db': '5055e1c619914eef979fef011fa5b912',
        'scout_id': 'mvoronovubervrz',
        'scout_name': u'uber воронов',
        'uuid': '5055e1c619914eef979fef011fa5b912_mvoronovubervrz',
        'transaction_id': '02abc5f6746a9554071bc937bf425a0f14'
    }

    if transaction_dt is None:
        transaction_dt = datetime(dt.year, dt.month, dt.day)

    balalayka_stat_params = {'price': price,
                             'dt': dt,
                             'transaction_dt': transaction_dt,
                             'client_id': client_id,
                             'transaction_id': str(transaction_id.int),
                             'currency': currency,
                             'payment_type': 'scout',
                             'service_id': Services.SCOUTS.id,
                             'transaction_type': 'payment',
                             'payload': json.dumps(payload)}

    query = """
        insert into t_partner_payment_stat 
          (id, price, dt, transaction_dt, client_id, transaction_id, currency, payment_type, service_id, transaction_type, payload) 
        values
          (s_partner_payment_stat_id.nextval, :price, :dt, :transaction_dt, :client_id, :transaction_id, :currency, :payment_type, :service_id, :transaction_type, :payload)
    """
    db.balance().execute(query, balalayka_stat_params)

    return id, transaction_id


@pytest.fixture(scope="module")
def fixtures():
    amount = 1200
    dt = datetime.now()
    client_id = check_steps.create_client()

    data = namedtuple('data', 'amount dt client_id')
    data_list = data(
        amount=amount,
        dt=dt,
        client_id=client_id,
    )

    return data_list


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TBS)
def test_without_diff(shared_data, fixtures):
    """

    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['scouts_data', 'client_id']) as before:
        before.validate()

        f = fixtures

        amount = f.amount
        dt = f.dt
        client_id = f.client_id
        _, transaction_id = create_sidepayment(amount, dt, client_id, 'RUB')

        scouts_data = [{
            "client_id": str(client_id),
            "amount": amount,
            "transaction_type": "payment",
            "service_id": str(Services.SCOUTS.id),
            "payment_type": "scout",
            "payload": "",
            "currency": "rub",
            "transaction_id": str(transaction_id.hex),
            "dt": dt.strftime("%Y-%m-%dT%H:%M:%SZ"),
        }]

    cmp_data = shared_steps.SharedBlocks.run_tbs(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    result = [(row['transaction_id'])
              for row in cmp_data
              if int(row['transaction_id']) == int(scouts_data[0]['transaction_id'], 16)]
    reporter.log("Result = %s" % result)

    butils.check_that(result, empty())


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TBS)
def test_not_found_in_taxi(shared_data, fixtures):
    """

    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['scouts_data', 'client_id']) \
            as before:
        before.validate()

        f = fixtures

        amount = f.amount
        dt = f.dt
        client_id = f.client_id
        _, transaction_id = create_sidepayment(amount, dt, client_id, 'RUB')

        scouts_data = None

    cmp_data = shared_steps.SharedBlocks.run_tbs(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    result = [(row['billing_client_id'], row['state'])
              for row in cmp_data if row['billing_client_id'] == client_id]
    reporter.log("Result = %s" % result)

    butils.check_that((client_id, 1), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TBS)
def test_not_found_in_billing(shared_data, fixtures):
    """

    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['scouts_data', 'client_id']) as before:
        before.validate()

        f = fixtures

        amount = f.amount
        dt = f.dt
        client_id = f.client_id

        transaction_id = uuid.uuid4()

        scouts_data = [{
            "client_id": str(client_id),
            "amount": amount,
            "transaction_type": "payment",
            "service_id": str(Services.SCOUTS.id),
            "payment_type": "scout",
            "payload": "",
            "currency": "rub",
            "transaction_id": str(transaction_id.hex),
            "dt": dt.strftime("%Y-%m-%dT%H:%M:%SZ")
        }]

    cmp_data = shared_steps.SharedBlocks.run_tbs(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = {}".format(cmp_data))

    result = [(row['taxi_client_id'], row['state'])
              for row in cmp_data if row['taxi_client_id'] == client_id]
    reporter.log("Result = {}".format(result))

    butils.check_that((client_id, 2), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TBS)
def test_amount_not_converge(shared_data, fixtures):
    """

    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['scouts_data', 'client_id']) as before:
        before.validate()

        f = fixtures

        amount = f.amount
        dt = f.dt
        client_id = f.client_id
        _, transaction_id = create_sidepayment(amount, dt, client_id, 'RUB')

        scouts_data = [{
            "client_id": str(client_id),
            "amount": amount + 150,
            "transaction_type": "payment",
            "service_id": str(Services.SCOUTS.id),
            "payment_type": "scout",
            "payload": "",
            "currency": "rub",
            "transaction_id": str(transaction_id.hex),
            "dt": dt.strftime("%Y-%m-%dT%H:%M:%SZ")
        }]

    cmp_data = shared_steps.SharedBlocks.run_tbs(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = {}".format(cmp_data))

    result = [(row['taxi_client_id'], row['state'])
              for row in cmp_data if row['taxi_client_id'] == client_id]
    reporter.log("Result = {}".format(result))

    butils.check_that((client_id, 3), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TBS)
def test_service_not_converge(shared_data, fixtures):
    """

    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['scouts_data', 'client_id']) as before:
        before.validate()

        f = fixtures

        amount = f.amount
        dt = f.dt
        client_id = f.client_id
        _, transaction_id = create_sidepayment(amount, dt, client_id, 'RUB')

        scouts_data = [{
            "client_id": str(client_id),
            "amount": amount,
            "transaction_type": "payment",
            "service_id": str(111),
            "payment_type": "scout",
            "payload": "",
            "currency": "rub",
            "transaction_id": str(transaction_id.hex),
            "dt": dt.strftime("%Y-%m-%dT%H:%M:%SZ")
        }]

    cmp_data = shared_steps.SharedBlocks.run_tbs(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = {}".format(cmp_data))

    result = [(row['taxi_client_id'], row['state'])
              for row in cmp_data if row['taxi_client_id'] == client_id]
    reporter.log("Result = {}".format(result))

    butils.check_that((client_id, 4), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TBS)
def test_client_not_converge(shared_data, fixtures):
    """

    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['scouts_data', 'client_id',
                                                                'client_id_missmatch']) as before:
        before.validate()

        f = fixtures

        amount = f.amount
        dt = f.dt
        client_id = f.client_id
        client_id_missmatch = check_steps.create_client()
        _, transaction_id = create_sidepayment(amount, dt, client_id, 'RUB')

        scouts_data = [{
            "client_id": str(client_id_missmatch),
            "amount": amount,
            "transaction_type": "payment",
            "service_id": str(Services.SCOUTS.id),
            "payment_type": "scout",
            "payload": "",
            "currency": "rub",
            "transaction_id": str(transaction_id.hex),
            "dt": dt.strftime("%Y-%m-%dT%H:%M:%SZ")
        }]

    cmp_data = shared_steps.SharedBlocks.run_tbs(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = {}".format(cmp_data))

    result = [(row['taxi_client_id'], row['state'])
              for row in cmp_data if row['taxi_client_id'] == client_id_missmatch]
    reporter.log("Result = {}".format(result))

    butils.check_that((client_id_missmatch, 5), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TBS)
def test_dt_not_converge(shared_data, fixtures):
    """

    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['scouts_data', 'client_id']) as before:
        before.validate()

        f = fixtures

        amount = f.amount
        dt = f.dt
        client_id = f.client_id
        _, transaction_id = create_sidepayment(amount, dt, client_id, 'RUB')

        scouts_data = [{
            "client_id": str(client_id),
            "amount": amount,
            "transaction_type": "payment",
            "service_id": str(Services.SCOUTS.id),
            "payment_type": "scout",
            "payload": "",
            "currency": "rub",
            "transaction_id": str(transaction_id.hex),
            "dt": (f.dt + timedelta(days=1)).strftime("%Y-%m-%dT%H:%M:%SZ")
        }]

    cmp_data = shared_steps.SharedBlocks.run_tbs(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = {}".format(cmp_data))

    result = [(row['taxi_client_id'], row['state'])
              for row in cmp_data if row['taxi_client_id'] == client_id]
    reporter.log("Result = {}".format(result))

    butils.check_that((client_id, 6), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TBS)
def test_dt_not_converge_wo_diff(shared_data, fixtures):
    """

    """
    # подробнее в тикете CHECK-2791
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['scouts_data', 'client_id']) as before:
        before.validate()

        f = fixtures

        amount = f.amount
        dt = f.dt
        client_id = f.client_id
        _, transaction_id = create_sidepayment(amount, dt, client_id, 'RUB')

        scouts_data = [{
            "client_id": str(client_id),
            "amount": amount,
            "transaction_type": "payment",
            "service_id": str(Services.SCOUTS.id),
            "payment_type": "scout",
            "payload": "",
            "currency": "rub",
            "transaction_id": str(transaction_id.hex),
            "dt": (f.dt + timedelta(hours=5)).strftime("%Y-%m-%dT%H:%M:%SZ")
        }]

    cmp_data = shared_steps.SharedBlocks.run_tbs(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = {}".format(cmp_data))

    result = [(row['transaction_id'])
              for row in cmp_data
              if int(row['transaction_id']) == int(scouts_data[0]['transaction_id'], 16)]
    reporter.log("Result = {}".format(result))

    butils.check_that(result, empty())


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TBS)
def test_transaction_type_not_converge(shared_data, fixtures):
    """

    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['scouts_data', 'client_id']) as before:
        before.validate()

        f = fixtures

        amount = f.amount
        dt = f.dt
        client_id = f.client_id
        _, transaction_id = create_sidepayment(amount, dt, client_id, 'RUB')

        scouts_data = [{
            "client_id": str(client_id),
            "amount": amount,
            "transaction_type": "test",
            "service_id": str(Services.SCOUTS.id),
            "payment_type": "scout",
            "payload": "",
            "currency": "rub",
            "transaction_id": str(transaction_id.hex),
            "dt": dt.strftime("%Y-%m-%dT%H:%M:%SZ")
        }]

    cmp_data = shared_steps.SharedBlocks.run_tbs(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = {}".format(cmp_data))

    result = [(row['taxi_client_id'], row['state'])
              for row in cmp_data if row['taxi_client_id'] == client_id]
    reporter.log("Result = {}".format(result))

    butils.check_that((client_id, 7), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TBS)
def test_payment_type_not_converge(shared_data, fixtures):
    """

    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['scouts_data', 'client_id']) as before:
        before.validate()

        f = fixtures

        amount = f.amount
        dt = f.dt
        client_id = f.client_id
        _, transaction_id = create_sidepayment(amount, dt, client_id, 'RUB')

        scouts_data = [{
            "client_id": str(client_id),
            "amount": amount,
            "transaction_type": "payment",
            "service_id": str(Services.SCOUTS.id),
            "payment_type": "test",
            "payload": "",
            "currency": "rub",
            "transaction_id": str(transaction_id.hex),
            "dt": dt.strftime("%Y-%m-%dT%H:%M:%SZ")
        }]

    cmp_data = shared_steps.SharedBlocks.run_tbs(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = {}".format(cmp_data))

    result = [(row['taxi_client_id'], row['state'])
              for row in cmp_data if row['taxi_client_id'] == client_id]
    reporter.log("Result = {}".format(result))

    butils.check_that((client_id, 8), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TBS)
def test_currency_not_converge(shared_data, fixtures):
    """

    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['scouts_data', 'client_id']) as before:
        before.validate()

        f = fixtures

        amount = f.amount
        dt = f.dt
        client_id = f.client_id
        _, transaction_id = create_sidepayment(amount, dt, client_id, 'RUB')

        scouts_data = [{
            "client_id": str(client_id),
            "amount": amount,
            "transaction_type": "payment",
            "service_id": str(Services.SCOUTS.id),
            "payment_type": "scout",
            "payload": "",
            "currency": "usd",
            "transaction_id": str(transaction_id.hex),
            "dt": dt.strftime("%Y-%m-%dT%H:%M:%SZ")
        }]

    cmp_data = shared_steps.SharedBlocks.run_tbs(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = {}".format(cmp_data))

    result = [(row['taxi_client_id'], row['state'])
              for row in cmp_data if row['taxi_client_id'] == client_id]
    reporter.log("Result = {}".format(result))

    butils.check_that((client_id, 9), is_in(result))


"""
Убрана проверка "test_transaction_not_uniq_in_taxi"
В ней создавалось две одинаковых записи в YT.
Решено убрать, т.к два следующих теста заодно проверяют и это
"""


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TBS)
def test_transaction_not_uniq_in_taxi_many(shared_data, fixtures):
    """

    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['scouts_data', 'client_id']) as before:
        before.validate()

        f = fixtures

        amount = f.amount
        dt = f.dt
        client_id = f.client_id
        _, transaction_id = create_sidepayment(amount, dt, client_id, 'RUB')

        base = {
            "client_id": str(client_id),
            "amount": amount,
            "transaction_type": "payment",
            "service_id": str(Services.SCOUTS.id),
            "payment_type": "scout",
            "payload": "",
            "currency": "usd",
            "transaction_id": str(transaction_id.hex),
            "dt": dt.strftime("%Y-%m-%dT%H:%M:%SZ")
        }

        scouts_data_1 = scouts_data_2 = scouts_data_3 = dict(base)
        scouts_data = [scouts_data_1, scouts_data_2, scouts_data_3]

    cmp_data = shared_steps.SharedBlocks.run_tbs(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = {}".format(cmp_data))

    result = [(row['taxi_client_id'], row['state'])
              for row in cmp_data if row['taxi_client_id'] == client_id]
    reporter.log("Result = {}".format(result))

    butils.check_that((client_id, 10), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TBS)
def test_not_found_in_billing_and_not_uniq(shared_data, fixtures):
    """

    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['scouts_data', 'client_id']) as before:
        before.validate()

        f = fixtures

        amount = f.amount
        dt = f.dt
        client_id = f.client_id
        transaction_id = uuid.uuid4()

        base = {
            "client_id": str(client_id),
            "amount": amount,
            "transaction_type": "payment",
            "service_id": str(Services.SCOUTS.id),
            "payment_type": "scout",
            "payload": "",
            "currency": "usd",
            "transaction_id": str(transaction_id.hex),
            "dt": dt.strftime("%Y-%m-%dT%H:%M:%SZ")
        }

        scouts_data_1 = scouts_data_2 = dict(base)
        scouts_data = [scouts_data_1, scouts_data_2]

    cmp_data = shared_steps.SharedBlocks.run_tbs(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = {}".format(cmp_data))

    result = [(row['taxi_client_id'], row['state'])
              for row in cmp_data if row['taxi_client_id'] == client_id]
    reporter.log("Result = {}".format(result))

    butils.check_that((client_id, 2), is_in(result))
    butils.check_that((client_id, 10), is_in(result))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TBS)
def test_inconsistent_dt_without_diff(shared_data, fixtures):
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['scouts_data', 'client_id']) as before:
        before.validate()

        f = fixtures

        amount = f.amount

        dt = f.dt - timedelta(days=6)
        transaction_dt = datetime(f.dt.year, f.dt.month, f.dt.day)

        client_id = f.client_id
        _, transaction_id = create_sidepayment(amount, dt, client_id, 'RUB',
                                               transaction_dt=transaction_dt)

        scouts_data = [{
            "client_id": str(client_id),
            "amount": amount,
            "transaction_type": "payment",
            "service_id": str(Services.SCOUTS.id),
            "payment_type": "scout",
            "payload": "",
            "currency": "rub",
            "transaction_id": str(transaction_id.hex),
            "dt": dt.strftime("%Y-%m-%dT%H:%M:%SZ"),
        }]

    cmp_data = shared_steps.SharedBlocks.run_tbs(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = %s" % cmp_data)

    result = [(row['transaction_id'])
              for row in cmp_data
              if int(row['transaction_id']) == int(scouts_data[0]['transaction_id'], 16)]
    reporter.log("Result = %s" % result)

    butils.check_that(result, empty())


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TBS)
def test_payload_transaction_id_taxi_billing(shared_data, fixtures):
    """

    """
    with CheckSharedBefore(shared_data=shared_data, cache_vars=['scouts_data', 'client_id']) as before:
        before.validate()

        f = fixtures

        amount = f.amount
        dt = f.dt
        client_id = check_steps.create_client()
        _, transaction_id = create_sidepayment(amount, dt, client_id, 'RUB')

        scouts_data = [{
            "client_id": str(client_id),
            "amount": amount + 150,
            "transaction_type": "payment",
            "service_id": str(Services.SCOUTS.id),
            "payment_type": "scout",
            "payload": "{\"transaction_id\": \"0abcbd6f12f12a7cad2c985357a751d2\"}",
            "currency": "rub",
            "transaction_id": str(transaction_id.hex),
            "dt": dt.strftime("%Y-%m-%dT%H:%M:%SZ")
        }]

    cmp_data = shared_steps.SharedBlocks.run_tbs(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    reporter.log("CMP_DATA = {}".format(cmp_data))

    # Проверяем, что проставились данные из payload: transaction_id
    payload_taxi_transaction_id = "0abcbd6f12f12a7cad2c985357a751d2"
    payload_billing_transaction_id = "02abc5f6746a9554071bc937bf425a0f14"

    cmp_data_taxi_payload_transaction_id = str()
    cmp_data_billing_payload_transaction_id = str()

    for row in cmp_data:
        if row['billing_client_id'] == client_id:
            cmp_data_taxi_payload_transaction_id = row['taxi_payload_transaction_id']
            cmp_data_billing_payload_transaction_id = row['billing_payload_transaction_id']
            break

    butils.check_that(cmp_data_taxi_payload_transaction_id,
                      has_string(payload_taxi_transaction_id))
    butils.check_that(cmp_data_billing_payload_transaction_id,
                      has_string(payload_billing_transaction_id))


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_TBS)
def test_check_diffs_count(shared_data):
    """
        В тесте проверяем, что общее количество росхождений в тестах совпадает с ожидаемым
    """
    diffs_count = 13

    with CheckSharedBefore(shared_data=shared_data, cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_tbs(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    butils.check_that((cmp_data), has_length(diffs_count))
