# coding=utf-8

from datetime import datetime

from hamcrest import empty

from btestlib import reporter, utils
from balance import balance_steps as steps
from btestlib.constants import PaymentType
from btestlib.data.partner_contexts import TRAVEL_ELECTRIC_TRAINS_RU_CONTEXT, Decimal, TransactionType
from btestlib.matchers import equal_to_casted_dict, contains_dicts_with_entries

START_DT = utils.Date.nullify_time_of_date(datetime.now())
PAYMENT_AMOUNT = Decimal('100.2')
REFUND_AMOUNT = Decimal('40.1')
context = TRAVEL_ELECTRIC_TRAINS_RU_CONTEXT

_, _, month1_start_dt, month1_end_dt, month2_start_dt, month2_end_dt = \
    utils.Date.previous_three_months_start_end_dates()


def create_ids_for_payment(context):
    with reporter.step(u'Создаем договор для клиента-партнера'):
        client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context, additional_params={
            'start_dt': month1_start_dt})

        return client_id, person_id, contract_id


def create_completions(context, client_id, contract_id, person_id, dt, coef=Decimal('1')):
    sum = steps.SimpleApi.create_fake_tpt_data(context, client_id, person_id, contract_id, dt, [
        {'transaction_type': TransactionType.PAYMENT, 'amount': PAYMENT_AMOUNT * coef, 'yandex_reward': 0,
         'payment_type': PaymentType.COST, 'payout_ready_dt': month1_start_dt},
        {'transaction_type': TransactionType.REFUND, 'amount': REFUND_AMOUNT * coef, 'yandex_reward': 0,
         'payment_type': PaymentType.COST, 'payout_ready_dt': month1_start_dt},
        {'transaction_type': TransactionType.PAYMENT, 'amount': 0, 'yandex_reward': PAYMENT_AMOUNT * coef,
         'payment_type': PaymentType.REWARD, 'payout_ready_dt': month1_start_dt},
        {'transaction_type': TransactionType.REFUND, 'amount': 0, 'yandex_reward': REFUND_AMOUNT * coef,
         'payment_type': PaymentType.REWARD, 'payout_ready_dt': month1_start_dt}
    ], sum_key='yandex_reward')
    return sum


def test_tech_travel_electric_act_wo_data():
    client_id, person_id, contract_id = create_ids_for_payment(context)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month2_start_dt,
                                                                   manual_export=False)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(context, contract_id,
                                                                                     person_id, Decimal('0'),
                                                                                     dt=month1_start_dt)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, empty(),
                     'Сравниваем данные из акта с шаблоном')


def test_tech_travel_electric_act_second_month():
    client_id, person_id, contract_id = create_ids_for_payment(context)

    first_month_sum = create_completions(context, client_id, contract_id, person_id, month1_start_dt,
                                         coef=Decimal('10'))
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month1_start_dt)

    second_month_sum_1 = create_completions(context, client_id, contract_id, person_id, month1_start_dt,
                                            coef=Decimal('10'))
    second_month_sum_2 = create_completions(context, client_id, contract_id, person_id, month2_start_dt,
                                            coef=Decimal('10'))
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month2_start_dt)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    # создаем шаблон для сравнения
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(
        context, contract_id,
        person_id,
        first_month_sum + second_month_sum_1 + second_month_sum_2,
        dt=month1_start_dt)

    expected_act_data = [
        steps.CommonData.create_expected_act_data(first_month_sum, month1_end_dt),
        steps.CommonData.create_expected_act_data(second_month_sum_1 + second_month_sum_2, month2_end_dt)
    ]

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')
