# -*- coding: utf-8 -*-

__author__ = 'yuelyasheva'

import datetime
from decimal import Decimal as D
from collections import defaultdict

import pytest
from hamcrest import empty

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features, AuditFeatures
from btestlib import utils
from btestlib.constants import TransactionType, NdsNew, Services, Pages, Firms, SpendablePaymentType
from btestlib.data.partner_contexts import CORP_TAXI_KZ_CONTEXT_SPENDABLE_MIGRATED, \
    CORP_TAXI_RU_CONTEXT_SPENDABLE_MIGRATED, CORP_TAXI_ARM_CONTEXT_SPENDABLE_MIGRATED, \
    CORP_TAXI_ISRAEL_CONTEXT_SPENDABLE_MIGRATED, CORP_TAXI_BY_CONTEXT_SPENDABLE_DECOUP, \
    TAXI_CORP_YANGO_ISRAEL_CONTEXT_SPENDABLE, CORP_TAXI_KGZ_CONTEXT_SPENDABLE
from btestlib.matchers import contains_dicts_with_entries

previous_month_start_dt, previous_month_end_dt = utils.Date.previous_month_first_and_last_days()
prev_quarter_start_dt, prev_quarter_end_dt = utils.Date.get_previous_quarter(datetime.datetime.today())
prev_quarter_last_month_start_dt = prev_quarter_end_dt.replace(day=1)

payment_sum = D('6000.9')
refund_sum = D('2000.1')

CLIENT_AMOUNT = D('1000')

PAGES_MAP = {
    Services.TAXI_CORP_PARTNERS.id: [
        Pages.CORP_TAXI_PARTNERS,
        Pages.CORP_TAXI_CARGO,
        Pages.CORP_TAXI_DELIVERY
    ],
    Services.TAXI_CORP.id: [
        Pages.CORP_TAXI
    ]
}

PAGES_COEF = {
    Pages.CORP_TAXI_PARTNERS: D('1.1'),
    Pages.CORP_TAXI_CARGO: D('1.2'),
    Pages.CORP_TAXI_DELIVERY: D('1.3'),
    Pages.CORP_TAXI: D('1')
}

RU_CONTEXT = CORP_TAXI_RU_CONTEXT_SPENDABLE_MIGRATED
RU_NEW_ONLY_SERVICE_CONTEXT = RU_CONTEXT.new(
    service=Services.TAXI_CORP_PARTNERS,
    contract_services=[Services.TAXI_CORP_PARTNERS.id]
)
RU_ZERO_NDS_CONTEXT = RU_CONTEXT.new(nds=NdsNew.ZERO)

KZ_CONTEXT = CORP_TAXI_KZ_CONTEXT_SPENDABLE_MIGRATED
KZ_ZERO_NDS_CONTEXT = KZ_CONTEXT.new(nds=NdsNew.ZERO)

ARM_CONTEXT = CORP_TAXI_ARM_CONTEXT_SPENDABLE_MIGRATED

ILS_CONTEXT = CORP_TAXI_ISRAEL_CONTEXT_SPENDABLE_MIGRATED

KGZ_CONTEXT = CORP_TAXI_KGZ_CONTEXT_SPENDABLE


@reporter.feature(Features.TAXI, Features.SPENDABLE, Features.CORPORATE, Features.PARTNER, Features.ACT)
@pytest.mark.audit(reporter.feature(AuditFeatures.RV_C10_1_Taxi))
@pytest.mark.tickets('BALANCE-22114')
@pytest.mark.parametrize('context',
                         [
                             pytest.mark.smoke(RU_NEW_ONLY_SERVICE_CONTEXT),
                             pytest.mark.smoke(RU_CONTEXT),
                             pytest.mark.smoke(RU_ZERO_NDS_CONTEXT),
                             KZ_CONTEXT,
                             KZ_ZERO_NDS_CONTEXT,
                             ILS_CONTEXT,
                             TAXI_CORP_YANGO_ISRAEL_CONTEXT_SPENDABLE,
                             CORP_TAXI_BY_CONTEXT_SPENDABLE_DECOUP,
                             KGZ_CONTEXT
                         ],
                         ids=[
                             'Acts for month with russian nds, RUS, new corp partners',
                             'Acts for month with russian nds, RUS, new and old corp partners',
                             'Acts for month with nds 0, RUS',
                             'Acts for month with kazakhstan nds, KZ',
                             'Acts for month with nds 0, KZ',
                             'Acts for month with israeli nds, ILS',
                             'Acts for month with YANGO israeli nds, YANGO_ILS',
                             'Acts for month with Belarus nds, BY',
                             'Acts for month with KGS nds, SOM'
                         ])
def test_corp_taxi_spendable_act(context):
    start_dt = previous_month_start_dt
    act_dt = utils.Date.last_day_of_month(start_dt)

    client_id, contract_id, data = generate_data(context, start_dt, start_dt, act_dt, SpendablePaymentType.MONTHLY)
    expected_act_data = create_expected_act(context, client_id, contract_id, start_dt, act_dt)
    utils.check_that(data, contains_dicts_with_entries(expected_act_data), u'Сравниваем данные из акта с шаблоном')


@pytest.mark.parametrize('context, start_dt, completions_dt, act_dt',
                         [
                             (RU_CONTEXT, prev_quarter_start_dt, prev_quarter_start_dt, prev_quarter_end_dt),
                             (RU_CONTEXT, prev_quarter_start_dt, prev_quarter_last_month_start_dt, prev_quarter_end_dt),
                             (KZ_CONTEXT, prev_quarter_start_dt, prev_quarter_last_month_start_dt, prev_quarter_end_dt),
                             (ILS_CONTEXT, prev_quarter_start_dt, prev_quarter_last_month_start_dt, prev_quarter_end_dt),
                             (TAXI_CORP_YANGO_ISRAEL_CONTEXT_SPENDABLE, prev_quarter_start_dt, prev_quarter_last_month_start_dt, prev_quarter_end_dt),
                             (CORP_TAXI_BY_CONTEXT_SPENDABLE_DECOUP,
                              prev_quarter_start_dt, prev_quarter_last_month_start_dt, prev_quarter_end_dt),
                         ],
                         ids=[
                             'Acts for the last month of period, RUS',
                             'Acts for the last month of period, contract start date = last month of period, RUS',
                             'Acts for the last month of period, contract start date = last month of period, KZ',
                             'Acts for the last month of period, contract start date = last month of period, ILS',
                             'Acts for the last month of period, contract start date = last month of period, YANGO_ILS',
                             'Acts for the last month of period, contract start date = last month of period, BY'
                         ])
def test_quarterly_acts(context, start_dt, completions_dt, act_dt):
    client_id, contract_id, data = generate_data(context, start_dt, completions_dt, act_dt,
                                                 SpendablePaymentType.QUARTERLY)
    expected_act_data = create_expected_act(context, client_id, contract_id, start_dt, act_dt)
    utils.check_that(data, contains_dicts_with_entries(expected_act_data), u'Сравниваем данные из акта с шаблоном')


@pytest.mark.parametrize('context', [
    RU_CONTEXT,
    KZ_CONTEXT,
    ILS_CONTEXT,
    CORP_TAXI_BY_CONTEXT_SPENDABLE_DECOUP,
    TAXI_CORP_YANGO_ISRAEL_CONTEXT_SPENDABLE,
],
    ids=['RUS', 'KZ', 'ILS', 'BY', 'YANGO_ILS'])
def test_first_month_of_quarter_generation(context):
    start_dt = prev_quarter_start_dt
    act_dt = utils.Date.last_day_of_month(start_dt)

    _, _, data = generate_data(context, start_dt, start_dt, act_dt, SpendablePaymentType.QUARTERLY)
    utils.check_that(data, empty(), u'Проверяем, что данных нет')


@reporter.feature(Features.TAXI, Features.SPENDABLE, Features.CORPORATE, Features.PARTNER, Features.ACT)
def test_corp_taxi_spendable_incomplete_month():
    AMOUNTS = [{'amount': D('1000.1'), 'dt': previous_month_start_dt.replace(day=9)},
              {'amount': D('200.2'), 'dt': previous_month_start_dt.replace(day=10)},
              {'amount': D('30.3'), 'dt': previous_month_start_dt.replace(day=20)},
              {'amount': D('4.4'), 'dt': previous_month_start_dt.replace(day=21)}]
    context = RU_CONTEXT
    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context,
                                                    additional_params={
                                                                        'start_dt': previous_month_start_dt.replace(day=10),
                                                                        'end_dt': previous_month_start_dt.replace(day=20),
                                                                        'nds': NdsNew.NOT_RESIDENT.nds_id
                                                    })
    for amount in AMOUNTS:
        steps.SimpleApi.create_fake_tpt_row(context, client_id, person_id, contract_id, amount['dt'],
                                            amount=amount['amount'], transaction_type=TransactionType.PAYMENT)

    # договор действует с 10 по 20 число, учитываем только открутки за 10 и 20 число
    month_sum = utils.dround2(AMOUNTS[1]['amount'] + AMOUNTS[2]['amount'])

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, previous_month_start_dt)
    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    expected_act_data = [steps.CommonData.create_expected_pad(context, client_id, contract_id, previous_month_start_dt,
                                                              nds=NdsNew.NOT_RESIDENT, partner_reward=month_sum)]

    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data), u'Сравниваем данные из акта с шаблоном')


@pytest.mark.tickets('BALANCE-30924')
def test_arm_no_act():
    context = ARM_CONTEXT
    start_dt = previous_month_start_dt
    act_dt = utils.Date.last_day_of_month(start_dt)

    _, _, data = generate_data(context, start_dt, start_dt, act_dt, SpendablePaymentType.MONTHLY)
    utils.check_that(data, empty(), u'Проверяем, что данных нет')


def generate_data(context, start_dt, completions_dt, act_dt, payment_type):
    client_id, person_id, contract_id = create_contact(context, start_dt, payment_type)

    create_completions(context, client_id, person_id, contract_id, completions_dt)

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, act_dt)

    data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
    return client_id, contract_id, data


def create_contact(context, start_dt, payment_type=SpendablePaymentType.MONTHLY):
    params = {
        'start_dt': start_dt,
        'nds': context.nds.nds_id,
        'payment_type': payment_type
    }

    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, additional_params=params)

    return client_id, person_id, contract_id


def create_completions(context, client_id, person_id, contract_id, dt):
    for service in context.contract_services:
        create_transactions(context, client_id, person_id, contract_id, dt, service)

        # internal платежи только в Казахстане
        if context.firm == Firms.TAXI_CORP_KZT_31:
            create_transactions(context, client_id, person_id, contract_id, dt, service, internal=1)


def create_expected_act(context, client_id, contract_id, start_dt, act_dt):
    expected_amount_by_each_payment_type = D(payment_sum - refund_sum) / context.nds.koef_on_dt(act_dt)

    pages_coefs = defaultdict(int)

    # мапим любые платежи вне зависимости от payment_type в Page.CORP_TAXI_PARTNERS
    # (так реализован процесс перехода с 135 сервиса на 651)
    for service_id in context.contract_services:
        target_page = Pages.CORP_TAXI_PARTNERS if service_id == Services.TAXI_CORP.id else None
        for page in PAGES_MAP[service_id]:
            pages_coefs[target_page or page] += PAGES_COEF[page]

    return [
        steps.CommonData.create_expected_pad(context, client_id, contract_id, start_dt,
                                             partner_reward=round(count * expected_amount_by_each_payment_type, 5),
                                             nds=context.nds,
                                             description=page.desc,
                                             page_id=page.id,
                                             type_id=context.pad_type_id,
                                             end_dt=act_dt)
        for page, count in pages_coefs.iteritems()
    ]


def create_transactions(context, client_id, person_id, contract_id, dt, service, internal=None):
    rows = []

    for page in PAGES_MAP[service]:
        rows += [
            {
                'client_amount': CLIENT_AMOUNT,
                'amount': PAGES_COEF[page] * payment_sum,
                'transaction_type': TransactionType.PAYMENT,
                'internal': internal,
                'service_id': service,
                'payment_type': page.payment_type
            },
            {
                'client_amount': CLIENT_AMOUNT,
                'amount': PAGES_COEF[page] * refund_sum,
                'transaction_type': TransactionType.REFUND,
                'internal': internal,
                'service_id': service,
                'payment_type': page.payment_type
            }
        ]

    steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id, dt, rows)
