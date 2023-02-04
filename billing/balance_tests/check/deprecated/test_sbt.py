# coding: utf-8
__author__ = 'chihiro'

from datetime import datetime
from decimal import Decimal
import time
import uuid

import pytest
from dateutil.relativedelta import relativedelta
from hamcrest import contains_string, equal_to
import yt.wrapper as yt
import yt.yson as yson
from startrek_client import Startrek

from balance import balance_steps as steps
from btestlib import utils, secrets
from btestlib.constants import Services, Nds
from btestlib.data import simpleapi_defaults
import btestlib.reporter as reporter
from simpleapi.common.payment_methods import Coupon, Subsidy
from btestlib.data.partner_contexts import TAXI_RU_CONTEXT, TAXI_BV_LAT_EUR_CONTEXT
from check import db
from btestlib import shared
from check import shared_steps
from check.shared import CheckSharedBefore
from check import utils as check_utils
from balance import balance_api as api
from check.defaults import STARTREK_PARAMS


START_DT = utils.Date.first_day_of_month() - relativedelta(months=1)
ORDER_DT = utils.Date.moscow_offset_dt()

context_ru = TAXI_RU_CONTEXT

PAYMETHODS = [
    Coupon(),
    Subsidy()
]

DIFFS_COUNT = 20

def create_general_client_and_contract(context, finish_dt=None):
    client_id, service_product_id = steps.SimpleApi.create_partner_and_product(Services.TAXI_DONATE)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    contract_id, external_id = steps.ContractSteps.create_contract('taxi_postpay', utils.remove_empty({
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'DT': START_DT,
        'IS_SIGNED': START_DT.isoformat(),
        'PARTNER_COMMISSION_PCT2': context.commission_pct,
        'SERVICES': [service for service in context.contract_services],
        'FIRM': context.firm.id,
        'CURRENCY': context.currency.num_code,
        'COUNTRY': context.region.id,
        'FINISH_DT': finish_dt
    }))
    return client_id, contract_id, service_product_id


def create_client_and_contract(context, finish_dt=None):
    client_id, contract_id, service_product_id = create_general_client_and_contract(context, finish_dt)

    partner_person_id = steps.PersonSteps.create(client_id, context.person_type.code, {'is-partner': '1'})
    partner_contract_id, _ = steps.ContractSteps.accept_taxi_offer(partner_person_id, contract_id,
                                                                   nds=Nds.get(context.nds_id))

    return client_id, partner_person_id, partner_contract_id, service_product_id


def create_refunds(paymethod=Subsidy()):
    client_id, person_id, contract_id, service_product_id = create_client_and_contract(context_ru)

    service_order_id, trust_payment_id, purchase_token, payment_id = steps.SimpleApi.create_trust_payment(
        Services.TAXI_DONATE, service_product_id, paymethod=paymethod, user=simpleapi_defaults.USER_ANONYMOUS,
        currency=context_ru.currency, order_dt=ORDER_DT)

    trust_refund_id, refund_id = steps.SimpleApi.create_refund(Services.TAXI_DONATE, service_order_id, trust_payment_id)

    steps.CommonPartnerSteps.export_payment(refund_id)
    refund_data = db.get_payment_data(refund_id)[0]
    payment_data = db.get_payment_data(payment_id)[0]
    return client_id, trust_payment_id, trust_refund_id, payment_data, refund_data


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SBT)
def test_sbt_without_diff_subsidies(shared_data, switch_to_pg):
    # здесь везде по два теста на одну проверку test_..._subsidies и test_..._coupons
    # отличаются типом оплаты - разбиты на два теста для скорости

    # в этом тесте проверяем, что платеж без расхождений с типом отплаты субсидии не попал в сверку
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'trust_payment_id', 'trust_refund_id', 'payment_data', 'refund_data', 'currency',
                        'paymethod']
    ) as before:
        before.validate()

        client_id, trust_payment_id, trust_refund_id, payment_data, refund_data = create_refunds()
        currency = 'RUB'
        paymethod = 'subsidies'
    cmp_data = shared_steps.SharedBlocks.run_sbt(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    # в одном и том же тесте проверяем, что в расхождения попал и платеж, и рефанд
    assert trust_payment_id not in [row['id'] for row in cmp_data]
    assert trust_refund_id not in [row['id'] for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SBT)
def test_sbt_without_diff_coupons(shared_data, switch_to_pg):
    # в этом тесте проверяем, что платеж без расхождений с типом отплаты купоны не попал в сверку
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'trust_payment_id', 'trust_refund_id', 'payment_data', 'refund_data', 'currency',
                        'paymethod']
    ) as before:
        before.validate()

        client_id, trust_payment_id, trust_refund_id, payment_data, refund_data = create_refunds(Coupon())
        currency = 'RUB'
        paymethod = 'coupons'
    cmp_data = shared_steps.SharedBlocks.run_sbt(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    assert trust_payment_id not in [row['id'] for row in cmp_data]
    assert trust_refund_id not in [row['id'] for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SBT)
def test_sbt_not_found_in_yt_subsidies(shared_data, switch_to_pg):
    # в этом тесте проверяем тип расхождений Отсутствует в YT
    # для этого создаем платеж, но в yt его не добавляем
    # добавление в yt происходит в шаге cmp_data = shared_steps.SharedBlocks.run_sbt
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'trust_payment_id', 'trust_refund_id', 'payment_data', 'refund_data', 'currency',
                        'paymethod']
    ) as before:
        before.validate()

        client_id, trust_payment_id, trust_refund_id, payment_data, refund_data = create_refunds()
        currency = 'RUB'
        paymethod = 'subsidies'
    cmp_data = shared_steps.SharedBlocks.run_sbt(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    assert (trust_payment_id, 1) in [(row['id'], row['state']) for row in cmp_data if row['id'] == trust_payment_id]
    assert (trust_refund_id, 1) in [(row['id'], row['state']) for row in cmp_data if row['id'] == trust_refund_id]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SBT)
def test_sbt_not_found_in_yt_coupons(shared_data, switch_to_pg):
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'trust_payment_id', 'trust_refund_id', 'payment_data', 'refund_data', 'currency',
                        'paymethod']
    ) as before:
        before.validate()

        client_id, trust_payment_id, trust_refund_id, payment_data, refund_data = create_refunds(Coupon())
        currency = 'RUB'
        paymethod = 'coupons'
    cmp_data = shared_steps.SharedBlocks.run_sbt(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    assert (trust_payment_id, 1) in [(row['id'], row['state']) for row in cmp_data if row['id'] == trust_payment_id]
    assert (trust_refund_id, 1) in [(row['id'], row['state']) for row in cmp_data if row['id'] == trust_refund_id]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SBT)
def test_sbt_not_found_in_billing_subsidies(shared_data, switch_to_pg):
    # в этом тесте проверяем тип расхождений Отсутствует в биллинге
    # для этого создаем только в yt
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'trust_payment_id', 'trust_refund_id', 'payment_data', 'refund_data', 'currency',
                        'paymethod']
    ) as before:
        before.validate()
        trust_payment_id = str(uuid.uuid4()).replace('-', '')[0:24]
        trust_refund_id = str(uuid.uuid4()).replace('-', '')[0:24]
        client_id, _ = steps.SimpleApi.create_partner_and_product(Services.TAXI_DONATE)
        payment_data = {'payment_dt': datetime.now(), 'amount': '111'}
        refund_data = {'payment_dt': datetime.now(), 'amount': '111'}
        currency = 'RUB'
        paymethod = 'subsidies'
    cmp_data = shared_steps.SharedBlocks.run_sbt(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    assert (trust_payment_id, 2) in [(row['id'], row['state']) for row in cmp_data if row['id'] == trust_payment_id]
    assert (trust_refund_id, 2) in [(row['id'], row['state']) for row in cmp_data if row['id'] == trust_refund_id]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SBT)
def test_sbt_not_found_in_billing_coupons(shared_data, switch_to_pg):
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'trust_payment_id', 'trust_refund_id', 'payment_data', 'refund_data', 'currency',
                        'paymethod']
    ) as before:
        before.validate()

        trust_payment_id = str(uuid.uuid4()).replace('-', '')[0:24]
        trust_refund_id = str(uuid.uuid4()).replace('-', '')[0:24]
        client_id, person_id, contract_id, service_product_id = create_client_and_contract(context_ru)
        payment_data = {'payment_dt': datetime.now(), 'amount': '111'}
        refund_data = {'payment_dt': datetime.now(), 'amount': '111'}
        currency = 'RUB'
        paymethod = 'coupons'
    cmp_data = shared_steps.SharedBlocks.run_sbt(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    assert (trust_payment_id, 2) in [(row['id'], row['state']) for row in cmp_data if row['id'] == trust_payment_id]
    assert (trust_refund_id, 2) in [(row['id'], row['state']) for row in cmp_data if row['id'] == trust_refund_id]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SBT)
def test_sbt_client_not_converge_subsidies(shared_data, switch_to_pg):
    # в этом тесте проверяем тип расхождений Расходится клиент
    # для этого создаем платеж, но в yt добавляем его с другим клиентом
    # добавление в yt происходит в шаге cmp_data = shared_steps.SharedBlocks.run_sbt
    # важно, что измененного клиента надо не только сохранить в yt,
    # но и передать в запуск сверки (см. check/shared_steps.py:81)
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'trust_payment_id', 'trust_refund_id', 'payment_data', 'refund_data', 'currency',
                        'paymethod', 'client_id_changed']
    ) as before:
        before.validate()

        client_id, trust_payment_id, trust_refund_id, payment_data, refund_data = create_refunds()
        client_id_changed, _ = steps.SimpleApi.create_partner_and_product(Services.TAXI_DONATE)
        currency = 'RUB'
        paymethod = 'subsidies'
    cmp_data = shared_steps.SharedBlocks.run_sbt(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    assert (trust_payment_id, 3) in [(row['id'], row['state']) for row in cmp_data if row['id'] == trust_payment_id]
    assert (trust_refund_id, 3) in [(row['id'], row['state']) for row in cmp_data if row['id'] == trust_refund_id]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SBT)
def test_sbt_client_not_converge_coupons(shared_data, switch_to_pg):
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'trust_payment_id', 'trust_refund_id', 'payment_data', 'refund_data', 'currency',
                        'paymethod', 'client_id_changed']
    ) as before:
        before.validate()

        client_id, trust_payment_id, trust_refund_id, payment_data, refund_data = create_refunds(Coupon())
        client_id_changed, _ = steps.SimpleApi.create_partner_and_product(Services.TAXI_DONATE)
        currency = 'RUB'
        paymethod = 'coupons'
    cmp_data = shared_steps.SharedBlocks.run_sbt(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    assert (trust_payment_id, 3) in [(row['id'], row['state']) for row in cmp_data if row['id'] == trust_payment_id]
    assert (trust_refund_id, 3) in [(row['id'], row['state']) for row in cmp_data if row['id'] == trust_refund_id]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SBT)
def test_sbt_currency_not_converge_subsidies(shared_data, switch_to_pg):
    # в этом тесте проверяем тип расхождений Расходится валюта
    # для этого создаем платеж, но в yt добавляем его с другим клиентом
    # чтобы добавилась другая валюта, необходимо подменить ее в кеше (см. check/tests/test_sbt.py:284)
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'trust_payment_id', 'trust_refund_id', 'payment_data', 'refund_data', 'currency',
                        'paymethod']
    ) as before:
        before.validate()

        client_id, trust_payment_id, trust_refund_id, payment_data, refund_data = create_refunds()
        currency = 'USD'
        paymethod = 'subsidies'
    cmp_data = shared_steps.SharedBlocks.run_sbt(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    assert (trust_payment_id, 4) in [(row['id'], row['state']) for row in cmp_data if row['id'] == trust_payment_id]
    assert (trust_refund_id, 4) in [(row['id'], row['state']) for row in cmp_data if row['id'] == trust_refund_id]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SBT)
def test_sbt_currency_not_converge_coupons(shared_data, switch_to_pg):
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'trust_payment_id', 'trust_refund_id', 'payment_data', 'refund_data', 'currency',
                        'paymethod']
    ) as before:
        before.validate()

        client_id, trust_payment_id, trust_refund_id, payment_data, refund_data = create_refunds(Coupon())
        currency = 'USD'
        paymethod = 'coupons'
    cmp_data = shared_steps.SharedBlocks.run_sbt(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    assert (trust_payment_id, 4) in [(row['id'], row['state']) for row in cmp_data if row['id'] == trust_payment_id]
    assert (trust_refund_id, 4) in [(row['id'], row['state']) for row in cmp_data if row['id'] == trust_refund_id]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SBT)
def test_sbt_amount_not_converge_subsidies(shared_data, switch_to_pg):
    # в этом тесте проверяем тип расхождений Расходится сумма
    # для этого создаем платеж, но в yt добавляем его с другой суммой
    # чтобы добавилась другая сумма, необходимо подменить ее в кеше (см. check/tests/test_sbt.py:327)
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'trust_payment_id', 'trust_refund_id', 'payment_data', 'refund_data', 'currency',
                        'paymethod']
    ) as before:
        before.validate()

        client_id, trust_payment_id, trust_refund_id, payment_data, refund_data = create_refunds()
        payment_data['amount'] = '5610.01'
        refund_data['amount'] = '5610.01'
        currency = 'RUB'
        paymethod = 'subsidies'
    cmp_data = shared_steps.SharedBlocks.run_sbt(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    assert (trust_payment_id, 5) in [(row['id'], row['state']) for row in cmp_data if row['id'] == trust_payment_id]
    assert (trust_refund_id, 5) in [(row['id'], row['state']) for row in cmp_data if row['id'] == trust_refund_id]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SBT)
def test_sbt_amount_not_converge_coupons(shared_data, switch_to_pg):
    with CheckSharedBefore(
            shared_data=shared_data,
            cache_vars=['client_id', 'trust_payment_id', 'trust_refund_id', 'payment_data', 'refund_data', 'currency',
                        'paymethod']
    ) as before:
        before.validate()

        client_id, trust_payment_id, trust_refund_id, payment_data, refund_data = create_refunds(Coupon())
        payment_data['amount'] = '5610.01'
        refund_data['amount'] = '5610.01'
        currency = 'RUB'
        paymethod = 'coupons'
    cmp_data = shared_steps.SharedBlocks.run_sbt(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    assert (trust_payment_id, 5) in [(row['id'], row['state']) for row in cmp_data if row['id'] == trust_payment_id]
    assert (trust_refund_id, 5) in [(row['id'], row['state']) for row in cmp_data if row['id'] == trust_refund_id]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SBT)
def test_sbt_check_diffs_count(shared_data, switch_to_pg):
    # здесь проверяем, что для данного запуска не было обнаружено лишних расхождений
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_sbt(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')

    # 4 - тест на количество расхождений, тест для стартрека и 2 теста без расхождения
    assert len(cmp_data) == DIFFS_COUNT


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SBT)
def test_CHECK_2664_startrek(shared_data, switch_to_pg):
    # здесь проверяем, что произошел авторазбор расхождений - подробнее можно посмотреть в тикете CHECK-2664
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_sbt(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data if cmp_data or cmp_data == [] else shared_data.cache.get('cmp_data')
    query = 'select issue_key from sbt_cmp where id = {cmp_id}'.format(cmp_id=cmp_data[0]['cmp_id'])
    issue_key = api.test_balance().ExecuteSQL('cmp', query)[0][
        'issue_key']
    startrek = Startrek(**STARTREK_PARAMS)
    ticket = startrek.issues[issue_key]

    comments = list(ticket.comments.get_all())
    reporter.log(">>>>>>>>>>>>> comments = %s " % comments)

    # 6 комментариев = 5 расхождений + 1 коммент Ждем разбора в связанном тикете
    utils.check_that(len(comments), equal_to(6))
    check = 0

    def check_keys(text, trans_id, check, queue=u'TAXIDUTY'):
        utils.check_that(text, contains_string(u'азбираем в {}-'.format(queue)))
        utils.check_that(text, contains_string(trans_id))
        check += 1
        return check

    for i in range(len(comments)):
        comments_text = comments[i].text

        if u'Субсидии отсутствуют в такси' in comments_text:
            check = check_keys(comments_text, [row['id'] for row in cmp_data if row['state'] == 1][0], check)
        elif u'Субсидии отсутствуют в Биллинге' in comments_text:
            check = check_keys(
                comments_text, [row['id'] for row in cmp_data if row['state'] == 2][0], check, queue=u'PAYSUP')
        elif u'У субсидий расходится ID партнера' in comments_text:
            check = check_keys(comments_text, [row['id'] for row in cmp_data if row['state'] == 3][0], check)
        elif u'У субсидий расходится валюта' in comments_text:
            check = check_keys(comments_text, [row['id'] for row in cmp_data if row['state'] == 4][0], check)
        elif u'У субсидий расходится сумма' in comments_text:
            check = check_keys(comments_text, [row['id'] for row in cmp_data if row['state'] == 5][0], check)

    utils.check_that(check, equal_to(5))

    utils.check_that(len(list(ticket.links.get_all())), equal_to(5))

