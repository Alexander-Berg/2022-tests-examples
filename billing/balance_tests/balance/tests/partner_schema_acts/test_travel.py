# -*- coding: utf-8 -*-
__author__ = 'yuelyasheva'

from decimal import Decimal as D
import pytest

from balance import balance_steps as steps
from btestlib import utils
from btestlib.data.partner_contexts import TRAVEL_EXPEDIA_CONTEXT, TRAVEL_CONTEXT_RUB
from btestlib.constants import TransactionType, PaymentType
from hamcrest import empty
from btestlib.matchers import equal_to_casted_dict, contains_dicts_with_entries

_, _, month_before_prev_start_dt, month_before_prev_end_dt, prev_month_start_dt, prev_month_end_dt = \
    utils.Date.previous_three_months_start_end_dates()

AMOUNTS = [{'type': PaymentType.COST, 'payment_sum': D('110.13'), 'refund_sum': D('52.1')},
           {'type': PaymentType.REWARD, 'payment_sum': D('10.13'), 'refund_sum': D('4.8')}]


@pytest.mark.parametrize('context', [
    TRAVEL_EXPEDIA_CONTEXT,
    TRAVEL_CONTEXT_RUB,
])
def test_act_wo_data(context):
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'start_dt': prev_month_start_dt})
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, prev_month_start_dt,
                                                                   manual_export=False)

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    utils.check_that(act_data, empty(), step=u'Проверим, что акт не сгенерировался')


@pytest.mark.parametrize('context', [
    TRAVEL_EXPEDIA_CONTEXT,
    TRAVEL_CONTEXT_RUB,
])
def test_payout_ready_dt_acts_two_months(context):
    """По схеме с TravelLine нужно передавать платежи в ОЕБС сразу при оплате брони, но актить вознаграждение в момент
    выезда клиента из отеля. Для этого путешествия проставляют payout_ready_dt и месяц закрывается
    по payout_ready_dt, а не dt.

    В тесте проверяется закрытие двух месяцев."""

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context,
        additional_params={'start_dt': month_before_prev_start_dt})

    # Открутки в первом месяце без payout_ready_dt
    create_completions(context, client_id, person_id, contract_id, month_before_prev_start_dt,
                       payout_ready_dt=None)
    # Открутки в первом месяце с payout_ready_dt в первом месяце
    create_completions(context, client_id, person_id, contract_id, month_before_prev_start_dt,
                       payout_ready_dt=month_before_prev_start_dt)
    # Открутки в первом месяце с payout_ready_dt во втором
    create_completions(context, client_id, person_id, contract_id, month_before_prev_start_dt,
                       payout_ready_dt=prev_month_start_dt)

    # Акты за первый месяц
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month_before_prev_end_dt)

    # Открутки во втором месяце с payout_ready_dt во втором месяце
    create_completions(context, client_id, person_id, contract_id, prev_month_start_dt,
                       payout_ready_dt=prev_month_start_dt)

    # Акты за следующий месяц
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, prev_month_end_dt)

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    expected_act_data, expected_invoice_data = create_expected_data(context, contract_id, person_id,
                                                                    month_before_prev_end_dt,
                                                                    prev_month_end_dt, coef=2)

    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data), u'Сравниваем данные из акта с шаблоном')
    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')


def create_completions(context, client_id, person_id, contract_id, dt, payout_ready_dt, coef=1):
    for item in AMOUNTS:
        steps.SimpleApi.create_fake_tpt_data(
            context, client_id, person_id, contract_id, dt,
            [{'amount': coef * item['payment_sum'],
              'transaction_type': TransactionType.PAYMENT,
              'payment_type': item['type'],
              'yandex_reward': coef * item['payment_sum']
              if item['type'] == PaymentType.REWARD else None,
              'payout_ready_dt': payout_ready_dt},
             {'amount': coef * item['refund_sum'],
              'transaction_type': TransactionType.REFUND,
              'payment_type': item['type'],
              'yandex_reward': coef * item['refund_sum']
              if item['type'] == PaymentType.REWARD else None,
              'payout_ready_dt': payout_ready_dt}])


def create_expected_data(context, contract_id, person_id, dt_1, dt_2, coef=D('1')):
    for item in AMOUNTS:
        if item['type'] == PaymentType.REWARD:
            total_amount_1 = (item['payment_sum'] - item['refund_sum'])
            total_amount_2 = (item['payment_sum'] - item['refund_sum']) * coef
    act_data = [steps.CommonData.create_expected_act_data(total_amount_1, dt_1),
                steps.CommonData.create_expected_act_data(total_amount_2, dt_2)]
    invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context,
                                                                            contract_id, person_id,
                                                                            total_amount_1 + total_amount_2,
                                                                            dt=dt_1)
    return act_data, invoice_data
