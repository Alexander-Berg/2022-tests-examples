# coding: utf-8
__author__ = 'chihiro'

from decimal import Decimal
from collections import namedtuple

import pytest
from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps
from btestlib import utils
import btestlib.reporter as reporter
from btestlib.constants import Services, TransactionType, Nds, SpendablePaymentType, \
    Pages, Regions, Managers, ContractSubtype
from btestlib.data import defaults as default
from btestlib.data.simpleapi_defaults import ThirdPartyData
from btestlib.data.partner_contexts import LAVKA_COURIER_SPENDABLE_CONTEXT, \
            FOOD_COURIER_SPENDABLE_CONTEXT, SCOUTS_RU_CONTEXT
from temp.igogor.balance_objects import Contexts
import balance.balance_db as db
from check import shared_steps
from check.shared import CheckSharedBefore

FIRST_MONTH = utils.Date.first_day_of_month() - relativedelta(months=2)
SECOND_MONTH = FIRST_MONTH + relativedelta(months=1)

PASSPORT_ID = default.PASSPORT_UID

CONTEXTS = [
    Contexts.TAXI_RU_CONTEXT,
    Contexts.TAXI_BV_ARM_USD_CONTEXT,
    Contexts.TAXI_UBER_BY_CONTEXT,
    Contexts.TAXI_UBER_AZ_CONTEXT
]

AMOUNTS = [{'type': Pages.COUPON, 'payment_sum': Decimal('100.1'), 'refund_sum': Decimal('95.9')},
           {'type': Pages.SUBSIDY, 'payment_sum': Decimal('42.77'), 'refund_sum': Decimal('24.47')},
           {'type': Pages.BRANDING_SUBSIDY, 'payment_sum': Decimal('50.3'), 'refund_sum': Decimal('1.2')},
           {'type': Pages.GUARANTEE_FEE, 'payment_sum': Decimal('125.46'), 'refund_sum': Decimal('23.4')},
           {'type': Pages.TRIP_BONUS, 'payment_sum': Decimal('33.33'), 'refund_sum': Decimal('0.2')},
           {'type': Pages.PERSONAL_BONUS, 'payment_sum': Decimal('87.67'), 'refund_sum': Decimal('9.03')},
           {'type': Pages.DISCOUNT_TAXI, 'payment_sum': Decimal('45.5'), 'refund_sum': Decimal('3.8')},
           {'type': Pages.SUPPORT_COUPON, 'payment_sum': Decimal('1.2'), 'refund_sum': Decimal('0.6')},
           {'type': Pages.BOOKING_SUBSIDY, 'payment_sum': Decimal('204.54'), 'refund_sum': Decimal('3.5')}]

SCOUT_AMOUNT = Decimal('400.23')

PAYMENT_TYPES = ['booking_subsidy', 'branding_subsidy', 'coupon', 'discount_taxi', 'guarantee_fee', 'personnel_bonus',
                 'subsidy', 'support_coupon', 'trip_bonus']


def create_transaction_act(add_scouts=False):
    client_id, person_id, contract_id = create_client_and_contract(Contexts.TAXI_RU_CONTEXT, Nds.YANDEX_RESIDENT, add_scouts)

    if add_scouts:
        pages = {Pages.SCOUT_CARGO_SUBSIDY: Decimal('1')}
        create_transactions(SCOUTS_RU_CONTEXT, pages, client_id, person_id, contract_id,
                            FIRST_MONTH, monthly_coef=1)
    else:
        create_completions(Contexts.TAXI_RU_CONTEXT, client_id, person_id, contract_id, FIRST_MONTH)

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, FIRST_MONTH)
    return client_id, contract_id


def create_completions(context, client_id, person_id, contract_id, dt, coef=1):
    for item in AMOUNTS:
        steps.SimpleApi.create_fake_thirdparty_payment(ThirdPartyData.TAXI_DONATE, contract_id,
                                                       person_id, client_id, amount=coef * item['payment_sum'],
                                                       dt=dt, payment_type=item['type'].payment_type,
                                                       payment_currency=context.currency,
                                                       contract_currency=context.currency)

        steps.SimpleApi.create_fake_thirdparty_payment(ThirdPartyData.TAXI_DONATE, contract_id,
                                                       person_id, client_id, amount=coef * item['refund_sum'],
                                                       dt=dt, payment_type=item['type'].payment_type,
                                                       transaction_type=TransactionType.REFUND,
                                                       payment_currency=context.currency,
                                                       contract_currency=context.currency)


def create_client_and_contract(context, nds, add_scouts, start_dt=FIRST_MONTH, payment_type=SpendablePaymentType.MONTHLY,
                               remove_params=None):
    client_id, contract_id = create_general_client_and_contract(context, start_dt)

    services = [Services.TAXI_DONATE.id]
    if add_scouts:
        services.append(Services.SCOUTS.id)

    spendable_person_id = steps.PersonSteps.create(client_id, context.person_type.code, {'is-partner': '1'})
    spendable_contract_id, _ = steps.ContractSteps.create_contract('spendable_taxi_donate', {
        'CLIENT_ID': client_id,
        'PERSON_ID': spendable_person_id,
        'FIRM': context.firm.id,
        'DT': start_dt,
        'IS_SIGNED': start_dt.isoformat(),
        'NDS': str(Nds.get(nds)),
        'PAYMENT_TYPE': payment_type,
        'LINK_CONTRACT_ID': contract_id,
        'CURRENCY': context.currency.iso_num_code,
        'SERVICES': services,
        'COUNTRY': context.region.id
    }, remove_params=remove_params)

    return client_id, spendable_person_id, spendable_contract_id


def create_general_client_and_contract(context, start_dt):
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, context.person_type.code)

    contract_id, external_id = steps.ContractSteps.create_contract('taxi_postpay', {
        'CLIENT_ID': client_id,
        'PERSON_ID': person_id,
        'DT': start_dt,
        'IS_SIGNED': start_dt.isoformat(),
        'PARTNER_COMMISSION_PCT2': context.commission_pct,
        'SERVICES': [service.id for service in context.services],
        'FIRM': context.firm.id,
        'CURRENCY': context.currency.num_code,
        'COUNTRY': context.region.id
    })
    return client_id, contract_id


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SPA)
def test_spa_without_diff(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['client_id', 'contract_id']) as before:
        before.validate()

        client_id, contract_id = create_transaction_act()
    cmp_data = shared_steps.SharedBlocks.run_spa(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert contract_id not in [row['contract_id'] for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SPA)
def test_spa_not_found_act(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['client_id', 'contract_id']) as before:
        before.validate()
        client_id, contract_id = create_transaction_act()

        _, contract_id_new = create_general_client_and_contract(Contexts.TAXI_RU_CONTEXT, FIRST_MONTH)
        query = 'update t_partner_act_data set partner_contract_id = {} where partner_contract_id = {}'.format(
            contract_id_new, contract_id)
        db.balance().execute(query)
    cmp_data = shared_steps.SharedBlocks.run_spa(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')
    for pay_type in PAYMENT_TYPES:
        assert (contract_id, pay_type, 2) in [(row['contract_id'], row['payment_type'], row['state']) for row in
                                              cmp_data if row['contract_id'] == contract_id]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SPA)
def test_spa_not_found_trans(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['client_id', 'contract_id']) as before:
        before.validate()
        _, contract_id_old = create_transaction_act()

        client_id, contract_id = create_general_client_and_contract(Contexts.TAXI_RU_CONTEXT, FIRST_MONTH)
        query = 'update t_partner_act_data set partner_contract_id = {} where partner_contract_id = {}'.format(
            contract_id, contract_id_old)
        db.balance().execute(query)
    cmp_data = shared_steps.SharedBlocks.run_spa(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    for pay_type in PAYMENT_TYPES:
        assert (contract_id, pay_type, 1) in [(row['contract_id'], row['payment_type'], row['state']) for row in
                                              cmp_data if row['contract_id'] == contract_id]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SPA)
def test_spa_sum_not_converge(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['client_id', 'contract_id']) as before:
        before.validate()

        client_id, contract_id = create_transaction_act()
        query = 'update t_partner_act_data set partner_reward_wo_nds = \'203.559322033898305084745762712\' where partner_contract_id = {}'.format(
            contract_id)
        db.balance().execute(query)
    cmp_data = shared_steps.SharedBlocks.run_spa(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    for pay_type in PAYMENT_TYPES:
        assert (contract_id, pay_type, 3) in [(row['contract_id'], row['payment_type'], row['state']) for row in
                                              cmp_data if row['contract_id'] == contract_id]

# =========================== test services [619. 646, 664]================

PAYMENT_SUM = Decimal('6000.9')
REFUND_SUM = Decimal('2000.1')

def create_transactions(context, pages, client_id, person_id, contract_id, dt, monthly_coef):
    rows = []

    for page, coef in pages.iteritems():
        rows += [
            {
                'amount': monthly_coef * coef * PAYMENT_SUM,
                'transaction_type': TransactionType.PAYMENT,
                'payment_type': page.payment_type
            },
            {
                'amount': monthly_coef * coef * REFUND_SUM,
                'transaction_type': TransactionType.REFUND,
                'payment_type': page.payment_type
            }
        ]

    steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id, dt, rows)


LAVKA_COURIER_SPENDABLE_CONTEXT_NEW = LAVKA_COURIER_SPENDABLE_CONTEXT.new(
    region=Regions.RU,
    services=[Services.LAVKA_COURIER_SUBSIDY]
)

data = namedtuple('data', 'context pages')

food = data(context=FOOD_COURIER_SPENDABLE_CONTEXT,
            pages={Pages.FOOD_COUPON: Decimal('1'),
                   Pages.FOOD_SUBSIDY: Decimal('1.1'),
            })

lavka = data(context=LAVKA_COURIER_SPENDABLE_CONTEXT_NEW,
            pages={Pages.LAVKA_COUPON: Decimal('1'),
                   Pages.LAVKA_SUBSIDY: Decimal('1.1'),
            })

scouts = data(context=SCOUTS_RU_CONTEXT,
            pages={Pages.SCOUT_CARGO_SUBSIDY: Decimal('1')
            })


CONTEXTS_PAGES = [
    pytest.param(food.context, food.pages,
                 id='FOOD_COURIER_SUBSIDY({})'.format(Services.FOOD_COURIER_SUBSIDY.id)),
    pytest.param(lavka.context, lavka.pages,
                 id='LAVKA_COURIER_SUBSIDY({})'.format(Services.LAVKA_COURIER_SUBSIDY.id)),
]

def create_temp_client_and_contract(context, start_dt):
    client_id = steps.ClientSteps.create()

    is_partner = context.contract_type in (ContractSubtype.SPENDABLE, ContractSubtype.PARTNERS)
    person_id = steps.PersonSteps.create(client_id, context.person_type.code,
                                                             {'is-partner': str(1 if is_partner else 0)})

    params = {
        'currency': context.currency.char_code,
        'country': context.special_contract_params['country'],
        'manager_uid': Managers.SOME_MANAGER.uid,
        'signed': 1,
        'services': context.contract_services or [context.service.id],
        'firm_id': context.firm.id,
        'client_id': client_id,
        'person_id': person_id,
        'nds': context.nds.nds_id,
        'start_dt': start_dt
        }

    contract_id, contract_eid = steps.ContractSteps.create_common_contract(params)

    return client_id, contract_id


@pytest.mark.parametrize('context, pages', CONTEXTS_PAGES)
@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SPA)
def test_spa_services_without_diff(shared_data, context, pages):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['client_id', 'contract_id']) as before:
        before.validate()

        params = {'start_dt': FIRST_MONTH}

        client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, additional_params=params)

        create_transactions(context, pages, client_id, person_id, contract_id, FIRST_MONTH, monthly_coef=1)
        steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, FIRST_MONTH)


    cmp_data = shared_steps.SharedBlocks.run_spa(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert contract_id not in [row['contract_id'] for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SPA)
def test_spa_scouts_without_diff(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['client_id', 'contract_id']) as before:
        before.validate()

        client_id, contract_id = create_transaction_act(add_scouts=True)

    cmp_data = shared_steps.SharedBlocks.run_spa(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    assert contract_id not in [row['contract_id'] for row in cmp_data]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SPA)
def test_spa_food_sum_not_converge(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['client_id', 'contract_id']) as before:
        before.validate()

        params = {'start_dt': FIRST_MONTH}

        context = food.context
        pages = food.pages

        client_id, person_id, contract_id, _ = \
            steps.ContractSteps.create_partner_contract(context, additional_params=params)

        create_transactions(context, pages, client_id, person_id, contract_id, FIRST_MONTH, monthly_coef=1)
        steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, FIRST_MONTH)

        query = 'update t_partner_act_data set partner_reward_wo_nds = \'203.559322033898305084745762712\' where partner_contract_id = {}'.format(
            contract_id)
        db.balance().execute(query)

    cmp_data = shared_steps.SharedBlocks.run_spa(shared_data, before, pytest.active_tests)

    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    for p_type in food.pages.iterkeys():
        payment_type = p_type.payment_type
        assert (contract_id, payment_type, 3) in [(row['contract_id'], row['payment_type'], row['state']) for row in
                                              cmp_data if row['contract_id'] == contract_id]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SPA)
def test_spa_lavka_not_found_act(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['client_id', 'contract_id']) as before:
        before.validate()

        params = {
            'start_dt': FIRST_MONTH
        }

        context = lavka.context
        pages = lavka.pages

        client_id, person_id, contract_id, _ = \
            steps.ContractSteps.create_partner_contract(context, additional_params=params)

        create_transactions(context, pages, client_id, person_id, contract_id, FIRST_MONTH, monthly_coef=1)
        steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, FIRST_MONTH)


        _, contract_id_new = create_temp_client_and_contract(context, FIRST_MONTH)

        query = 'update t_partner_act_data set partner_contract_id = {} where partner_contract_id = {}'.format(
            contract_id_new, contract_id)
        db.balance().execute(query)

    cmp_data = shared_steps.SharedBlocks.run_spa(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    for p_type in lavka.pages.iterkeys():
        payment_type = p_type.payment_type
        assert (contract_id, payment_type, 2) in [(row['contract_id'], row['payment_type'], row['state']) for row in
                                              cmp_data if row['contract_id'] == contract_id]


@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SPA)
def test_spa_scouts_not_found_trans(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['client_id', 'contract_id']) as before:
        before.validate()
        _, contract_id_old = create_transaction_act(add_scouts=True)

        client_id, contract_id = create_general_client_and_contract(Contexts.TAXI_RU_CONTEXT, FIRST_MONTH)
        query = 'update t_partner_act_data set partner_contract_id = {} where partner_contract_id = {}'.format(
            contract_id, contract_id_old)
        db.balance().execute(query)
    cmp_data = shared_steps.SharedBlocks.run_spa(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    pages = scouts.pages

    for p_type in pages.iterkeys():
        payment_type = p_type.payment_type
        assert (contract_id, payment_type, 1) in [(row['contract_id'], row['payment_type'], row['state']) for row in
                                              cmp_data if row['contract_id'] == contract_id]

# ===========================End tests for services================

@pytest.mark.shared(block=shared_steps.SharedBlocks.RUN_SPA)
def test_spa_check_diffs_count(shared_data):
    with CheckSharedBefore(shared_data=shared_data,
                           cache_vars=['cache_var']) as before:
        before.validate()
        cache_var = 'test'

    cmp_data = shared_steps.SharedBlocks.run_spa(shared_data, before, pytest.active_tests)
    cmp_data = cmp_data or shared_data.cache.get('cmp_data')

    active_tests = [row for row in pytest.active_tests if 'test_spa.py' in row]
    # Отнимаем от активных тестов 8():
    # 4 - без расхождений,
    # 1 - текущая проверка,
    # 3 - расхождения для сервисов[619,646,664] - т.к. у ник другое количество payment_types(у Такси - 9)
    # Прибавляем 5: lavka - 2, food - 2, scouts - 1
    assert len(cmp_data) == (len(active_tests) - 8) * len(PAYMENT_TYPES) + 5
