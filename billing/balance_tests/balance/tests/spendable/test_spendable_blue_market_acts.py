# -*- coding: utf-8 -*-
__author__ = 'alshkit'

from decimal import Decimal

import pytest

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import NdsNew, TransactionType, PaysysType, PaymentType, Pages
from btestlib.data.partner_contexts import BLUE_MARKET_SUBSIDY, BLUE_MARKET_SUBSIDY_ISRAEL
from btestlib.matchers import contains_dicts_with_entries

import balance.balance_db as db
from balance.balance_steps.new_taxi_steps import TaxiSteps

# constants=============================================================================================================
first_month_start_dt, _, second_month_start_dt, _, third_month_start_dt, _ = \
    utils.Date.previous_three_months_start_end_dates()

PAYMENT_SUM_SUBSIDY = Decimal('106.32')
PAYMENT_SUM_SPASIBO = Decimal('239.10')
REFUND_SUM_SUBSIDY = Decimal('9.99')
REFUND_SUM_SPASIBO = Decimal('5.21')

CONTEXT_CODES = {
    'acc_subsidy': {
        'tpt_paysys_type_cc': PaysysType.MARKET,
        'tpt_payment_type': PaymentType.SUBSIDY,
        'payment_amount': Decimal('106.32'),
        'refund_amount': Decimal('9.99'),

        'page_id': Pages.BLUEMARKETSUBSIDY.id,
        'pad_description': Pages.BLUEMARKETSUBSIDY.desc,
    },
    'acc_plus': {
        'tpt_paysys_type_cc': PaysysType.MARKET_PLUS,
        'tpt_payment_type': PaymentType.YANDEX_ACCOUNT_WITHDRAW,
        'payment_amount': Decimal('239.10'),
        'refund_amount': Decimal('5.21'),

        'page_id': Pages.YANDEX_ACCOUNT_WITHDRAW.id,
        'pad_description': Pages.YANDEX_ACCOUNT_WITHDRAW.desc,
    },
    'pay_subsidy': {
        'tpt_paysys_type_cc': PaysysType.MARKET,
        'tpt_payment_type': PaymentType.PAY_SUBSIDY,
        'payment_amount': Decimal('999.99'),
        'refund_amount': Decimal('15.21'),

        'page_id': None,
        'pad_description': None,
    }
}

# utils=================================================================================================================


def create_context(orig_context, context_code):
    return orig_context.new(**CONTEXT_CODES[context_code])


def create_completions(client_id, person_id, contract_id, dt, contexts, coef=Decimal('1')):
    return [
        steps.SimpleApi.create_fake_tpt_data(
            context, client_id, person_id, contract_id, dt,
            [{'amount': context.payment_amount * coef,
              'transaction_type': TransactionType.PAYMENT},
             {'amount': context.refund_amount * coef,
              'transaction_type': TransactionType.REFUND}])
        for context in contexts
    ]


def get_reward(sum, nds, dt):
    return utils.dround(utils.dround2(sum) / nds.koef_on_dt(dt), 5)


# tests=================================================================================================================
# нарастающий итог за два месяца
@pytest.mark.parametrize(
    'context, nds',
    (
        pytest.param(BLUE_MARKET_SUBSIDY, NdsNew.DEFAULT, id='With Russia NDS'),
        pytest.param(BLUE_MARKET_SUBSIDY, NdsNew.ZERO, id='With Russia NDS=0%'),
        pytest.param(BLUE_MARKET_SUBSIDY_ISRAEL, NdsNew.ISRAEL, id='With Israel NDS'),
        pytest.param(BLUE_MARKET_SUBSIDY_ISRAEL, NdsNew.ZERO, id='With Israel NDS=0%'),
    ))
def test_bm_generate_act_two_month(context, nds):
    # context = create_context(context, 'acc_subsidy')
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={
            'start_dt': first_month_start_dt,
            'nds': nds.nds_id}
    )
    # pay_context, plus_context = (create_context(context, code) for code in ('pay_subsidy', 'acc_plus'))
    contexts = [create_context(context, code) for code in CONTEXT_CODES]
    # создадим открутки в одном месяце
    first_rewards = create_completions(client_id, person_id, contract_id, first_month_start_dt, contexts,
                                       coef=Decimal('1'))

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, first_month_start_dt)

    # создадим открутки в закрытом и незакрытом месяце
    second_rewards_1 = create_completions(client_id, person_id, contract_id, first_month_start_dt, contexts,
                                          coef=Decimal('0.3'))
    second_rewards_2 = create_completions(client_id, person_id, contract_id, second_month_start_dt, contexts,
                                          coef=Decimal('0.4'))

    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, second_month_start_dt)

    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    expected_data = [
        steps.CommonData.create_expected_pad(
            context, client_id, contract_id, first_month_start_dt,
            partner_reward=get_reward(reward, nds, first_month_start_dt), nds=nds
        )
        for reward, context in zip(first_rewards, contexts)
        if context.page_id
    ]
    expected_data.extend(
        steps.CommonData.create_expected_pad(
            context, client_id, contract_id, second_month_start_dt,
            partner_reward=get_reward(reward1 + reward2, nds, second_month_start_dt), nds=nds
        )
        for reward1, reward2, context in zip(second_rewards_1, second_rewards_2, contexts)
        if context.page_id
    )

    utils.check_that(act_data, contains_dicts_with_entries(expected_data),
                     step=u'Сравниваем данные в partner_act_data с ожидаемыми')


def test_tlog_timeline():
    context = BLUE_MARKET_SUBSIDY
    nds = NdsNew.DEFAULT
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={
            'start_dt': first_month_start_dt,
            'nds': nds.nds_id
        }
    )
    page_contexts = {cc['tpt_payment_type']: context.new(**cc) for cc in CONTEXT_CODES.values()}
    page_id_by_type = {
        d['payment_type']: d['page_id']
        for d in db.balance().execute(
            'select page_id, payment_type from t_partner_page where service_id=609')
    }
    expected_acts = []
    expected_timelines = []

    def get_sidepayment_thirdparty_id():
        return int(db.balance().execute(
            'SELECT 1324836609 + s_partner_payment_stat_id.nextval * 10 + 1 as val FROM dual'
        )[0]['val'])

    def add_compl(payment_type, amount, dt, transaction_id, transaction_type=TransactionType.PAYMENT, keep_trust_id=False):
        params = {
            'amount': amount,
            'transaction_type': transaction_type,
            'payment_type': payment_type,
            'trust_id': transaction_id,
        }
        if not keep_trust_id:
            params['id'] = get_sidepayment_thirdparty_id()

        steps.SimpleApi.create_fake_tpt_data(
            context, client_id, person_id, contract_id, dt,
            (params,)
        )

    def add_expected_act(payment_type, amount, dt):
        expected_acts.append(
            steps.CommonData.create_expected_pad(
                page_contexts[payment_type], client_id, contract_id, dt,
                partner_reward=get_reward(amount, nds, dt), nds=nds
            )
        )

    def add_expected_timeline(payment_type, last_transaction_id, act_dt):
        expected_timelines.append({
            'object_id': None,
            'classname': 'PartnerActData',
            'contract_id': contract_id,
            'operation_type': 'partner_act',
            'last_transaction_id': last_transaction_id,
            'pad_contract_id': contract_id,
            'pad_place_id': None,
            'pad_page_id': page_id_by_type[payment_type],
            'pad_dt': act_dt
        })

    def check():
        act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)
        utils.check_that(act_data, contains_dicts_with_entries(expected_acts),
                         step=u'Сравниваем данные в partner_act_data с ожидаемыми')

        tlog_notches = TaxiSteps.get_tlog_timeline_notch(contract_id=contract_id)
        utils.check_that(tlog_notches, contains_dicts_with_entries(expected_timelines),
                         u'Сравниваем данные в t_tlog_timeline с ожидаемыми')

    add_compl(PaymentType.SUBSIDY, 10, first_month_start_dt, transaction_id=666, keep_trust_id=True)
    add_compl(PaymentType.YANDEX_ACCOUNT_WITHDRAW, 100, first_month_start_dt, transaction_id='abcdef', keep_trust_id=True)
    add_compl(PaymentType.SUBSIDY, 90, first_month_start_dt, transaction_id=9)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, first_month_start_dt)
    # акт на 100 субсидия, id=9
    # акт на 100 плюсы, катоффа нет
    add_expected_act(PaymentType.SUBSIDY, 100, first_month_start_dt)
    add_expected_act(PaymentType.YANDEX_ACCOUNT_WITHDRAW, 100, first_month_start_dt)
    add_expected_timeline(PaymentType.SUBSIDY, 9, first_month_start_dt)
    check()

    add_compl(PaymentType.YANDEX_ACCOUNT_WITHDRAW, 200, second_month_start_dt, 23)
    add_compl(PaymentType.SUBSIDY, 200, second_month_start_dt, 65, TransactionType.REFUND)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, second_month_start_dt)
    # акт на 200 плюсы, id=23
    # субсидия в минусе, нет ни акта, ни катоффа
    add_expected_act(PaymentType.YANDEX_ACCOUNT_WITHDRAW, 200, second_month_start_dt)
    add_expected_timeline(PaymentType.YANDEX_ACCOUNT_WITHDRAW, 23, second_month_start_dt)
    check()

    add_compl(PaymentType.YANDEX_ACCOUNT_WITHDRAW, 300, third_month_start_dt, 123)
    add_compl(PaymentType.SUBSIDY, 300, third_month_start_dt, 342)
    steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id, third_month_start_dt)
    # акт на 300 плюсы, id=123
    # акт на 100 субсидия, id=342
    add_expected_act(PaymentType.YANDEX_ACCOUNT_WITHDRAW, 300, third_month_start_dt)
    add_expected_timeline(PaymentType.YANDEX_ACCOUNT_WITHDRAW, 123, third_month_start_dt)
    add_expected_act(PaymentType.SUBSIDY, 100, third_month_start_dt)
    add_expected_timeline(PaymentType.SUBSIDY, 342, third_month_start_dt)
    check()
