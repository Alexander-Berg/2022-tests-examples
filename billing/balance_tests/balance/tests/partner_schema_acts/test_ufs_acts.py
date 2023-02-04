# coding: utf-8
__author__ = 'a-vasin'

from decimal import Decimal

import pytest
from hamcrest import empty

import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features
from btestlib import reporter
from btestlib.constants import TransactionType, Products, PaymentType
from btestlib.data.partner_contexts import UFS_RU_CONTEXT
from btestlib.data.simpleapi_defaults import UFS_REFUND_FEE, UFS_PAYMENT_FEE, PaysysType
from btestlib.matchers import equal_to_casted_dict, contains_dicts_with_entries

pytestmark = [
    reporter.feature(Features.ACT, Features.UFS),
    pytest.mark.tickets('BALANCE-24893')
]

_, _, month1_start_dt, month1_end_dt, month2_start_dt, month2_end_dt = \
    utils.Date.previous_three_months_start_end_dates()

PAYMENT_AMOUNT = Decimal('100.2')
REFUND_AMOUNT = Decimal('40.1')
INSURANCE_REWARD_PAYMENT_AMOUNT = Decimal('63.1')
INSURANCE_REWARD_REFUND_AMOUNT = Decimal('17.7')


def create_ids_for_payment_ufs():
    with reporter.step(u'Создаем договор для клиента-партнера'):
        client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
            UFS_RU_CONTEXT,
            additional_params={'start_dt': month1_start_dt})

        return client_id, person_id, contract_id


def create_tech_ids_for_payment_ufs():
    with reporter.step(u'Создаем тех договор'):
        tech_client_id, tech_person_id, tech_contract_id = create_ids_for_payment_ufs()

        # обновляем тех клинта в t_config
        # steps.CommonPartnerSteps.update_t_config_ya_partner(Services.UFS, tech_client_id)

        return tech_client_id, tech_person_id, tech_contract_id


def create_completions_tech_contract(client_id, contract_id, person_id, dt, coef=Decimal('1')):
    sum = steps.SimpleApi.create_fake_tpt_data(UFS_RU_CONTEXT, client_id, person_id, contract_id, dt, [
        {'transaction_type': TransactionType.PAYMENT, 'amount': 0, 'yandex_reward': PAYMENT_AMOUNT * coef,
         'amount_fee': PAYMENT_AMOUNT * coef, 'internal': 1, 'payout_ready_dt': dt},
        {'transaction_type': TransactionType.REFUND, 'amount': REFUND_AMOUNT * coef,
         'yandex_reward': REFUND_AMOUNT * coef,
         'internal': 1, 'payout_ready_dt': dt},
    ], sum_key='yandex_reward')
    return sum


def create_ufs_no_insurance_completions(client_id, contract_id, person_id, dt):
    steps.SimpleApi.create_fake_tpt_data(UFS_RU_CONTEXT, client_id, person_id, contract_id, dt, [
        # Добавляем строки thirdparty с amount для выплаты
        {'transaction_type': TransactionType.PAYMENT, 'amount': PAYMENT_AMOUNT, 'payout_ready_dt': dt},
        {'transaction_type': TransactionType.REFUND, 'amount': REFUND_AMOUNT, 'payout_ready_dt': dt},
        # Добавляем строки thirdparty с amount_fee для удержания сервисного сбора
        {'transaction_type': TransactionType.PAYMENT, 'amount': 0, 'amount_fee': UFS_PAYMENT_FEE,
         'paysys_type_cc': PaysysType.YANDEX, 'payout_ready_dt': dt},
        {'transaction_type': TransactionType.REFUND, 'amount': 0, 'amount_fee': UFS_REFUND_FEE,
         'paysys_type_cc': PaysysType.YANDEX, 'payout_ready_dt': dt},
        # Добавляем строку для компенсации
        {'transaction_type': TransactionType.PAYMENT, 'payment_type': PaymentType.COMPENSATION,
         'paysys_type_cc': PaysysType.YANDEX, 'payout_ready_dt': dt}
    ])


def create_ufs_insurance_completions(client_id, contract_id, person_id, dt, coef=Decimal('1')):
    # Добавляем строки thirdparty с reward для удержания вознаграждения за страховку
    sum = steps.SimpleApi.create_fake_tpt_data(UFS_RU_CONTEXT, client_id, person_id, contract_id, dt, [
        {'transaction_type': TransactionType.PAYMENT, 'amount': PAYMENT_AMOUNT * coef,
         'yandex_reward': INSURANCE_REWARD_PAYMENT_AMOUNT * coef, 'paysys_type_cc': PaysysType.INSURANCE,
         'product_id': Products.UFS_INSURANCE_PAYMENTS.id, 'payout_ready_dt': dt},
        {'transaction_type': TransactionType.REFUND, 'amount': REFUND_AMOUNT * coef,
         'yandex_reward': INSURANCE_REWARD_REFUND_AMOUNT * coef, 'paysys_type_cc': PaysysType.INSURANCE,
         'product_id': Products.UFS_INSURANCE_PAYMENTS.id},
    ], sum_key='yandex_reward')
    # refund without payout_ready_dt, will not be included in act
    sum += INSURANCE_REWARD_REFUND_AMOUNT * coef
    return sum


# @pytest.mark.no_parallel('ufs')
# один тест, т.к. отличий между договорами тех клиента и партнера нет
def test_tech_ufs_act_wo_data():
    client_id, person_id, contract_id = create_tech_ids_for_payment_ufs()

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month2_start_dt,
                                                                   manual_export=False)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(
        UFS_RU_CONTEXT, contract_id,
        person_id, Decimal('0'),
        dt=month1_start_dt)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, empty(),
                     'Сравниваем данные из акта с шаблоном')

    # Перестали изменять id договора в t_thirdparty_service. Генерация актов ничего не знает про force_partner_id
    # Она просто делает акты по договорам, для которых есть что актить в tpt. Таким образом просто подкладывая
    # другого партнёра в tpt мы получим акты для этого договора.
    # TODO: Добавить тест на проверку фильтрации актов по тех.договорам в v_rep_agent_rep
    # Проверяем, что нет строки агентского вознаграждения
    # agent_rep_data = steps.CommonPartnerSteps.get_data_from_agent_rep(contract_id)
    # utils.check_that(agent_rep_data, empty(), 'Проверяем, что нет строки агентского вознаграждения по техническому договору')


# OFF Логика генерации актов для UFS не зависит от того какой договор закрывает: технический или нет.
# @pytest.mark.no_parallel('ufs')
# здесь проверяем закрытие с платежами сборка Яндекса (они на тех клиента)
def test_tech_ufs_act_second_month():
    client_id, person_id, contract_id = create_tech_ids_for_payment_ufs()

    first_month_sum = create_completions_tech_contract(client_id, contract_id, person_id, month1_start_dt,
                                                       coef=Decimal('1'))
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month1_start_dt)

    second_month_sum_1 = create_completions_tech_contract(client_id, contract_id, person_id, month1_start_dt,
                                                          coef=Decimal('0.3'))
    second_month_sum_2 = create_completions_tech_contract(client_id, contract_id, person_id, month2_start_dt,
                                                          coef=Decimal('0.4'))
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month2_start_dt)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    # создаем шаблон для сравнения
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(
        UFS_RU_CONTEXT, contract_id,
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


# проверка закрытия с платежами без страховок (когда нет вознаграждения и актов)
def test_ufs_act_first_month():
    client_id, person_id, contract_id = create_ids_for_payment_ufs()

    create_ufs_no_insurance_completions(client_id, contract_id, person_id, month2_start_dt)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month2_start_dt,
                                                                   manual_export=False)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(
        UFS_RU_CONTEXT, contract_id,
        person_id, Decimal('0'),
        dt=month1_start_dt)

    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     'Сравниваем данные из счета с шаблоном')
    utils.check_that(act_data, empty(),
                     'Сравниваем данные из акта с шаблоном')


@pytest.mark.smoke
# проверка закрытия с платежами со страховками
def test_ufs_act_insurance_second_month():
    client_id, person_id, contract_id = create_ids_for_payment_ufs()

    first_month_sum = create_ufs_insurance_completions(client_id, contract_id, person_id, month1_start_dt,
                                                       coef=Decimal('1'))
    create_ufs_no_insurance_completions(client_id, contract_id, person_id, month1_start_dt)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month1_start_dt)

    second_month_sum_1 = create_ufs_insurance_completions(client_id, contract_id, person_id, month1_start_dt,
                                                          coef=Decimal('0.3'))
    second_month_sum_2 = create_ufs_insurance_completions(client_id, contract_id, person_id, month2_start_dt,
                                                          coef=Decimal('0.4'))
    create_ufs_no_insurance_completions(client_id, contract_id, person_id, month2_start_dt)
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, month2_start_dt)

    # проверяем данные в счете
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]

    # проверяем данные в акте
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    # создаем шаблон для сравнения
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(
        UFS_RU_CONTEXT, contract_id,
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
