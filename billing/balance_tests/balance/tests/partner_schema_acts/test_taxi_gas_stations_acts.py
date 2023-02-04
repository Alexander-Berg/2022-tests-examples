# -*- coding: utf-8 -*-
__author__ = 'torvald'

from decimal import Decimal as D

import pytest
from hamcrest import empty

from balance import balance_steps as steps
from btestlib import utils

from btestlib.constants import Services, TransactionType, PaymentType, OEBSOperationType, \
    Export, PaysysType, ServiceCode, ActType, ContractSubtype
from balance.balance_steps import simple_api_steps
from btestlib.matchers import contains_dicts_with_entries, contains_dicts_equal_to
from btestlib.data.partner_contexts import TAXI_RU_CONTEXT, ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, \
    TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT, ZAXI_KZ_AGENT_CONTEXT, ZAXI_KZ_COMMISSION_CONTEXT, \
    ZAXI_DELIVERY_RU_CONTEXT, TAXI_RU_DELIVERY_CONTEXT
from balance import balance_db as db

_, _, MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, PREV_MONTH_START_DT, PREV_MONTH_END_DT = \
    utils.Date.previous_three_months_start_end_dates()


@pytest.mark.parametrize('taxi_context, zaxi_context, zaxi_spendable_context', [
    pytest.param(TAXI_RU_CONTEXT, ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_RU'),
    pytest.param(TAXI_RU_DELIVERY_CONTEXT, ZAXI_DELIVERY_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_DELIVERY_RU'),
])
def test_taxi_gas_stations_wo_data(taxi_context, zaxi_context, zaxi_spendable_context):
    client_id_taxopark, client_id_gas_station, person_id_taxopark, taxi_contract_id, _, \
    zaxi_contract_id, person_id_gas_station, zaxi_spendable_contract_id = \
        create_contracts(taxi_context, zaxi_context, zaxi_spendable_context)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id_taxopark, taxi_contract_id,
                                                                   MONTH_BEFORE_PREV_START_DT,
                                                                   manual_export=False)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id_taxopark, zaxi_contract_id,
                                                                   MONTH_BEFORE_PREV_START_DT,
                                                                   manual_export=False)

    if zaxi_spendable_context:
        steps.CommonPartnerSteps.generate_partner_acts_fair(zaxi_spendable_contract_id, MONTH_BEFORE_PREV_START_DT)

        acts_spendable = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(zaxi_spendable_contract_id)
        utils.check_that(acts_spendable, empty(),
                         'Сравниваем данные из акта с шаблоном')

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id_taxopark)
    expected_invoice_data = [
        steps.CommonData.create_expected_invoice_data_by_context(
            zaxi_context,
            zaxi_contract_id,
            person_id_taxopark,
            D('0'),
            dt=MONTH_BEFORE_PREV_START_DT),
        steps.CommonData.create_expected_invoice_data_by_context(
            taxi_context,
            taxi_contract_id,
            person_id_taxopark,
            D('0'),
            dt=MONTH_BEFORE_PREV_START_DT,
            paysys_id=taxi_context.additional_paysys.id)]
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, same_length=False),
                     'Сравниваем данные из счета с шаблоном')

    act_data_first_month = steps.ActsSteps.get_act_data_by_client(client_id_taxopark, internal=True)
    utils.check_that(act_data_first_month, empty(),
                     'Сравниваем данные из акта с шаблоном')


@pytest.mark.parametrize('taxi_context, zaxi_context, zaxi_spendable_context, need_taxi_act', [
    pytest.param(TAXI_RU_CONTEXT, ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, False, id='ZAXI_RU', marks=pytest.mark.smoke),
    pytest.param(TAXI_RU_DELIVERY_CONTEXT, ZAXI_DELIVERY_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, True, id='ZAXI_DELIVERY_RU'),
])
def test_taxi_gas_stations_increasing_total(taxi_context, zaxi_context, zaxi_spendable_context, need_taxi_act):
    deposit_payment = D('1000.11')
    fuel_fact_1 = D('82.12')
    fuel_fact_2 = D('93.17')
    completions_sum_1 = D('11.11')
    completions_sum_2 = D('22.22')
    deposit_payout_first_month = D('100.22')
    deposit_payout_second_month_1 = D('35.35')
    deposit_payout_second_month_2 = D('50.00')
    fuel_fact = D('27.88')

    client_id_taxopark, client_id_gas_station, person_id_taxopark, taxi_contract_id, _, \
    zaxi_contract_id, person_id_gas_station, zaxi_spendable_contract_id = \
        create_contracts(taxi_context, zaxi_context, zaxi_spendable_context)

    lsd_id, lsd_external_id, _ = steps.InvoiceSteps.get_invoice_by_service_or_service_code(taxi_contract_id,
                                                                                           Services.TAXI,
                                                                                           ServiceCode.DEPOSITION)
    lsz_id, lsz_external_id, _ = steps.InvoiceSteps.get_invoice_by_service_or_service_code(zaxi_contract_id,
                                                                                           Services.ZAXI,
                                                                                           ServiceCode.YANDEX_SERVICE)

    # -----------------------------------------------------------------------------------

    create_fake_deposit_payment(taxi_context, zaxi_context, zaxi_spendable_context,
                                client_id_taxopark, person_id_taxopark, taxi_contract_id,
                                amount=deposit_payment, invoice=lsd_external_id, dt=MONTH_BEFORE_PREV_START_DT)

    create_fake_fuel_fact(taxi_context, zaxi_context, zaxi_spendable_context,
                          client_id_taxopark, client_id_gas_station, person_id_taxopark,
                          person_id_gas_station, taxi_contract_id, zaxi_contract_id, zaxi_spendable_contract_id,
                          amount=completions_sum_1, LSD=lsd_external_id, LSZ=lsz_external_id,
                          dt=MONTH_BEFORE_PREV_START_DT)

    create_fake_deposit_payout(taxi_context, zaxi_context, zaxi_spendable_context,
                               client_id_taxopark, person_id_taxopark, taxi_contract_id,
                               amount=deposit_payout_first_month, invoice=lsd_external_id,
                               dt=MONTH_BEFORE_PREV_START_DT)

    create_and_export_fake_cpf_insert_fuel_hold(lsd_external_id, lsd_id, deposit_payment, MONTH_BEFORE_PREV_START_DT)
    create_and_export_fake_cpf_insert_fuel_fact(lsz_external_id, lsz_id, fuel_fact_1, MONTH_BEFORE_PREV_START_DT)

    steps.PartnerSteps.get_partner_balance(zaxi_context.service, [zaxi_contract_id])

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(
        client_id_taxopark,
        taxi_contract_id,
        MONTH_BEFORE_PREV_START_DT,
        manual_export=need_taxi_act,
    )

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(
        client_id_taxopark, zaxi_contract_id, MONTH_BEFORE_PREV_START_DT,
    )

    if zaxi_spendable_context:
        steps.CommonPartnerSteps.generate_partner_acts_fair(zaxi_spendable_contract_id, MONTH_BEFORE_PREV_START_DT)

    # -----------------------------------------------------------------------------------

    create_fake_fuel_fact(taxi_context, zaxi_context, zaxi_spendable_context,
                          client_id_taxopark, client_id_gas_station, person_id_taxopark,
                          person_id_gas_station, taxi_contract_id, zaxi_contract_id, zaxi_spendable_contract_id,
                          amount=fuel_fact, LSD=lsd_external_id, LSZ=lsz_external_id, dt=MONTH_BEFORE_PREV_START_DT)

    create_fake_deposit_payout(taxi_context, zaxi_context, zaxi_spendable_context,
                               client_id_taxopark, person_id_taxopark, taxi_contract_id,
                               amount=deposit_payout_second_month_1, invoice=lsd_external_id,
                               dt=MONTH_BEFORE_PREV_START_DT)

    create_fake_fuel_fact(taxi_context, zaxi_context, zaxi_spendable_context,
                          client_id_taxopark, client_id_gas_station, person_id_taxopark,
                          person_id_gas_station, taxi_contract_id, zaxi_contract_id, zaxi_spendable_contract_id,
                          amount=completions_sum_2, LSD=lsd_external_id, LSZ=lsz_external_id, dt=PREV_MONTH_START_DT)

    create_fake_deposit_payout(taxi_context, zaxi_context, zaxi_spendable_context,
                               client_id_taxopark, person_id_taxopark, taxi_contract_id,
                               amount=deposit_payout_second_month_2, invoice=lsd_external_id, dt=PREV_MONTH_START_DT)

    create_and_export_fake_cpf_insert_fuel_fact(lsz_external_id, lsz_id, fuel_fact_2, PREV_MONTH_START_DT)

    steps.PartnerSteps.get_partner_balance(zaxi_context.service, [zaxi_contract_id])

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id_taxopark, taxi_contract_id,
                                                                   PREV_MONTH_START_DT, manual_export=need_taxi_act)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id_taxopark, zaxi_contract_id,
                                                                   PREV_MONTH_START_DT)

    if zaxi_spendable_context:
        steps.CommonPartnerSteps.generate_partner_acts_fair(zaxi_spendable_contract_id, PREV_MONTH_START_DT)

        acts_spendable = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(zaxi_spendable_contract_id)
        utils.check_that(acts_spendable, empty(),
                         'Сравниваем данные из акта с шаблоном')

    if need_taxi_act:
        taxi_total_act_sum = deposit_payout_first_month + deposit_payout_second_month_1 + \
                             completions_sum_1 + completions_sum_2 + fuel_fact + deposit_payout_second_month_2
    else:
        taxi_total_act_sum = D('0')

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id_taxopark)
    expected_invoice_data = [
        steps.CommonData.create_expected_invoice_data_by_context(
            zaxi_context, zaxi_contract_id,
            person_id_taxopark,
            fuel_fact_1 + fuel_fact_2,
            dt=MONTH_BEFORE_PREV_START_DT,
            total_act_sum=completions_sum_1 + completions_sum_2 + fuel_fact,
            consume_sum=completions_sum_1 + completions_sum_2 + fuel_fact
        ),
        steps.CommonData.create_expected_invoice_data_by_context(
            taxi_context, taxi_contract_id,
            person_id_taxopark,
            deposit_payment,
            dt=MONTH_BEFORE_PREV_START_DT,
            paysys_id=taxi_context.additional_paysys.id,
            total_act_sum=taxi_total_act_sum,
            consume_sum=taxi_total_act_sum)
    ]
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, same_length=False),
                     'Сравниваем данные из счета с шаблоном')

    expected_act_data = [
        steps.CommonData.create_expected_act_data(completions_sum_1, MONTH_BEFORE_PREV_END_DT),
        steps.CommonData.create_expected_act_data(completions_sum_2 + fuel_fact, PREV_MONTH_END_DT),
    ]

    if need_taxi_act:
        expected_act_data.extend([
            steps.CommonData.create_expected_act_data(deposit_payout_first_month + completions_sum_1,
                                                      MONTH_BEFORE_PREV_END_DT, type=ActType.INTERNAL),
            steps.CommonData.create_expected_act_data(deposit_payout_second_month_1 + deposit_payout_second_month_2 +
                                                      completions_sum_2 + fuel_fact, PREV_MONTH_END_DT, type=ActType.INTERNAL)
        ])

    act_data_first_month = steps.ActsSteps.get_act_data_by_client(client_id_taxopark, internal=True)
    utils.check_that(act_data_first_month, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')


@pytest.mark.parametrize('taxi_context, zaxi_context, zaxi_spendable_context, need_taxi_act', [
    pytest.param(TAXI_RU_CONTEXT, ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, False, id='ZAXI_RU'),
    pytest.param(TAXI_RU_DELIVERY_CONTEXT, ZAXI_DELIVERY_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, True, id='ZAXI_DELIVERY_RU'),
])
def test_taxi_gas_stations_balances(taxi_context, zaxi_context, zaxi_spendable_context, need_taxi_act):
    deposit_payment = D('1000.11')
    fuel_fact = D('82.12')
    zaxi_sum = D('11.11')
    deposit_payout = D('100.22')

    client_id_taxopark, client_id_gas_station, person_id_taxopark, taxi_contract_id, taxi_contract_eid, \
    zaxi_contract_id, person_id_gas_station, zaxi_spendable_contract_id = \
        create_contracts(taxi_context, zaxi_context, zaxi_spendable_context)
    lsd_id, lsd_external_id, _ = steps.InvoiceSteps.get_invoice_by_service_or_service_code(
        taxi_contract_id,
        Services.TAXI,
        ServiceCode.DEPOSITION)
    lsz_id, lsz_external_id, _ = steps.InvoiceSteps.get_invoice_by_service_or_service_code(
        zaxi_contract_id,
        Services.ZAXI,
        ServiceCode.YANDEX_SERVICE)
    # проверяем, что в начале баланс нулевой
    balance = steps.PartnerSteps.get_partner_balance(zaxi_context.service, [zaxi_contract_id])
    expected_balance = balance_expected_data(taxi_context, zaxi_context, zaxi_spendable_context,
                                             taxi_contract_id, taxi_contract_eid, D('0'), D('0'), D('0'),
                                             lsz_external_id)
    utils.check_that(balance, contains_dicts_with_entries(expected_balance),
                     'Сравниваем баланс с шаблоном')

    create_fake_deposit_payment(taxi_context, zaxi_context, zaxi_spendable_context,
                                client_id_taxopark, person_id_taxopark, taxi_contract_id,
                                amount=deposit_payment, invoice=lsd_external_id, dt=MONTH_BEFORE_PREV_START_DT)
    create_fake_fuel_fact(taxi_context, zaxi_context, zaxi_spendable_context,
                          client_id_taxopark, client_id_gas_station, person_id_taxopark,
                          person_id_gas_station, taxi_contract_id, zaxi_contract_id, zaxi_spendable_contract_id,
                          amount=zaxi_sum, LSD=lsd_external_id, LSZ=lsz_external_id, dt=MONTH_BEFORE_PREV_START_DT)

    create_fake_deposit_payout(taxi_context, zaxi_context, zaxi_spendable_context,
                               client_id_taxopark, person_id_taxopark, taxi_contract_id,
                               amount=deposit_payout, invoice=lsd_external_id, dt=MONTH_BEFORE_PREV_START_DT)
    create_and_export_fake_cpf_insert_fuel_hold(lsd_external_id, lsd_id, deposit_payment, MONTH_BEFORE_PREV_START_DT)
    create_and_export_fake_cpf_insert_fuel_fact(lsz_external_id, lsz_id, fuel_fact, MONTH_BEFORE_PREV_START_DT)

    # проверяем, что баланс меняется после изменений депозита и откруток
    balance = steps.PartnerSteps.get_partner_balance(zaxi_context.service, [zaxi_contract_id])
    expected_balance = balance_expected_data(taxi_context, zaxi_context, zaxi_spendable_context,
                                             taxi_contract_id, taxi_contract_eid, deposit_payment, zaxi_sum,
                                             deposit_payout, lsz_external_id)
    utils.check_that(balance, contains_dicts_with_entries(expected_balance),
                     'Сравниваем баланс с шаблоном')

    balance = steps.PartnerSteps.get_partner_balance(zaxi_context.service, [zaxi_contract_id])
    expected_balance = balance_expected_data(taxi_context, zaxi_context, zaxi_spendable_context,
                                             taxi_contract_id, taxi_contract_eid, deposit_payment, zaxi_sum,
                                             deposit_payout, lsz_external_id)
    consumes_data = [db.get_consumes_by_invoice(lsd_id), db.get_consumes_by_invoice(lsz_id)]
    utils.check_that(balance, contains_dicts_with_entries(expected_balance),
                     'Сравниваем баланс с шаблоном')
    utils.check_that(consumes_data, [empty(), empty()], 'Проверяем, что нет консьюмов')

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(
        client_id_taxopark,
        taxi_contract_id,
        MONTH_BEFORE_PREV_START_DT,
        manual_export=need_taxi_act,
    )
    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(
        client_id_taxopark,
        zaxi_contract_id,
        MONTH_BEFORE_PREV_START_DT,
    )
    if zaxi_spendable_context:
        steps.CommonPartnerSteps.generate_partner_acts_fair(zaxi_spendable_contract_id, MONTH_BEFORE_PREV_START_DT)

    # проверяем, что баланс после актов рассчитывается правильно
    balance = steps.PartnerSteps.get_partner_balance(zaxi_context.service, [zaxi_contract_id])
    expected_balance = balance_expected_data(taxi_context, zaxi_context, zaxi_spendable_context,
                                             taxi_contract_id, taxi_contract_eid, deposit_payment, zaxi_sum,
                                             deposit_payout, lsz_external_id, is_act_generated=need_taxi_act)
    utils.check_that(balance, contains_dicts_with_entries(expected_balance),
                     'Сравниваем баланс с шаблоном')



@pytest.mark.parametrize('taxi_context, zaxi_context, gas_station_agent_context', [
    pytest.param(TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT, ZAXI_KZ_COMMISSION_CONTEXT, ZAXI_KZ_AGENT_CONTEXT, id='ZAXI_KZ')
])
def test_taxi_gas_stations_commission_scheme_wo_data(taxi_context, zaxi_context, gas_station_agent_context):
    client_id_taxopark, client_id_gas_station, person_id_taxopark, taxi_contract_id, _, \
    zaxi_contract_id, person_id_gas_station, gas_station_contract_id = \
        create_contracts(taxi_context, zaxi_context, gas_station_agent_context)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id_taxopark, taxi_contract_id,
                                                                   MONTH_BEFORE_PREV_START_DT,
                                                                   manual_export=False)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id_taxopark, zaxi_contract_id,
                                                                   MONTH_BEFORE_PREV_START_DT,
                                                                   manual_export=False)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id_gas_station, gas_station_contract_id,
                                                                   MONTH_BEFORE_PREV_START_DT,
                                                                   manual_export=False)

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id_taxopark)
    expected_invoice_data = [
        steps.CommonData.create_expected_invoice_data_by_context(
            zaxi_context,
            zaxi_contract_id,
            person_id_taxopark,
            D('0'),
            dt=MONTH_BEFORE_PREV_START_DT),
        steps.CommonData.create_expected_invoice_data_by_context(
            taxi_context,
            taxi_contract_id,
            person_id_taxopark,
            D('0'),
            dt=MONTH_BEFORE_PREV_START_DT,
            paysys_id=taxi_context.additional_paysys.id)]
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, same_length=False),
                     'Сравниваем данные из счета с шаблоном')

    act_data_first_month = steps.ActsSteps.get_act_data_by_client(client_id_taxopark, internal=True)
    utils.check_that(act_data_first_month, empty(),
                     'Сравниваем данные из акта с шаблоном')

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id_gas_station)
    expected_invoice_data = [
        steps.CommonData.create_expected_invoice_data_by_context(
            gas_station_agent_context,
            gas_station_contract_id,
            person_id_gas_station,
            D('0'),
            dt=MONTH_BEFORE_PREV_START_DT),
    ]
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, same_length=False),
                     'Сравниваем данные из счета с шаблоном')

    act_data_first_month = steps.ActsSteps.get_act_data_by_client(client_id_gas_station, internal=True)
    utils.check_that(act_data_first_month, empty(),
                     'Сравниваем данные из акта с шаблоном')


@pytest.mark.parametrize('taxi_context, zaxi_context, gas_station_agent_context, need_taxi_act', [
    pytest.param(TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT, ZAXI_KZ_COMMISSION_CONTEXT, ZAXI_KZ_AGENT_CONTEXT, True, id='ZAXI_KZ')
])
def test_taxi_gas_stations_commission_scheme_increasing_total(taxi_context, zaxi_context, gas_station_agent_context, need_taxi_act):
    deposit_payment = D('1000.11')
    fuel_fact_1 = D('82.12')
    gas_station_reward_1 = D('8.21')
    fuel_fact_2 = D('93.17')
    gas_station_reward_2 = D('9.32')
    completions_sum_1 = D('11.11')
    completions_sum_2 = D('22.22')
    deposit_payout_first_month = D('100.22')
    deposit_payout_second_month_1 = D('35.35')
    deposit_payout_second_month_2 = D('50.00')
    fuel_fact = D('27.88')

    client_id_taxopark, client_id_gas_station, person_id_taxopark, taxi_contract_id, _, \
    zaxi_contract_id, person_id_gas_station, gas_station_contract_id = \
        create_contracts(taxi_context, zaxi_context, gas_station_agent_context)

    lsd_id, lsd_external_id, _ = steps.InvoiceSteps.get_invoice_by_service_or_service_code(taxi_contract_id,
                                                                                           Services.TAXI,
                                                                                           ServiceCode.DEPOSITION)
    lsz_id, lsz_external_id, _ = steps.InvoiceSteps.get_invoice_by_service_or_service_code(zaxi_contract_id,
                                                                                           Services.ZAXI,
                                                                                           ServiceCode.YANDEX_SERVICE)
    gas_station_ls_id, gas_station_ls_external_id, _ = steps.InvoiceSteps.get_invoice_by_service_or_service_code(gas_station_contract_id,
                                                                                           Services.ZAXI_AGENT_COMMISSION,
                                                                                           ServiceCode.AGENT_REWARD)

    # -----------------------------------------------------------------------------------

    create_fake_deposit_payment(taxi_context, zaxi_context, gas_station_agent_context,
                                client_id_taxopark, person_id_taxopark, taxi_contract_id,
                                amount=deposit_payment, invoice=lsd_external_id, dt=MONTH_BEFORE_PREV_START_DT)

    create_fake_fuel_fact(taxi_context, zaxi_context, gas_station_agent_context,
                          client_id_taxopark, client_id_gas_station, person_id_taxopark,
                          person_id_gas_station, taxi_contract_id, zaxi_contract_id, gas_station_contract_id,
                          amount=completions_sum_1, LSD=lsd_external_id, LSZ=lsz_external_id,
                          dt=MONTH_BEFORE_PREV_START_DT, GAS_STATION_LS=gas_station_ls_external_id,
                          gas_station_reward=gas_station_reward_1)

    create_fake_deposit_payout(taxi_context, zaxi_context, gas_station_agent_context,
                               client_id_taxopark, person_id_taxopark, taxi_contract_id,
                               amount=deposit_payout_first_month, invoice=lsd_external_id,
                               dt=MONTH_BEFORE_PREV_START_DT)

    create_and_export_fake_cpf_insert_fuel_hold(lsd_external_id, lsd_id, deposit_payment, MONTH_BEFORE_PREV_START_DT)
    create_and_export_fake_cpf_insert_fuel_fact(lsz_external_id, lsz_id, fuel_fact_1, MONTH_BEFORE_PREV_START_DT)

    steps.PartnerSteps.get_partner_balance(zaxi_context.service, [zaxi_contract_id])

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(
        client_id_taxopark,
        taxi_contract_id,
        MONTH_BEFORE_PREV_START_DT,
        manual_export=need_taxi_act,
    )

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(
        client_id_taxopark, zaxi_contract_id, MONTH_BEFORE_PREV_START_DT,
    )

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(
        client_id_gas_station, gas_station_contract_id, MONTH_BEFORE_PREV_START_DT,
    )

    # -----------------------------------------------------------------------------------

    create_fake_fuel_fact(taxi_context, zaxi_context, gas_station_agent_context,
                          client_id_taxopark, client_id_gas_station, person_id_taxopark,
                          person_id_gas_station, taxi_contract_id, zaxi_contract_id, gas_station_contract_id,
                          amount=fuel_fact, LSD=lsd_external_id, LSZ=lsz_external_id, dt=MONTH_BEFORE_PREV_START_DT)

    create_fake_deposit_payout(taxi_context, zaxi_context, gas_station_agent_context,
                               client_id_taxopark, person_id_taxopark, taxi_contract_id,
                               amount=deposit_payout_second_month_1, invoice=lsd_external_id,
                               dt=MONTH_BEFORE_PREV_START_DT)

    create_fake_fuel_fact(taxi_context, zaxi_context, gas_station_agent_context,
                          client_id_taxopark, client_id_gas_station, person_id_taxopark,
                          person_id_gas_station, taxi_contract_id, zaxi_contract_id, gas_station_contract_id,
                          amount=completions_sum_2, LSD=lsd_external_id, LSZ=lsz_external_id, dt=PREV_MONTH_START_DT,
                          GAS_STATION_LS=gas_station_ls_external_id, gas_station_reward=gas_station_reward_2)

    create_fake_deposit_payout(taxi_context, zaxi_context, gas_station_agent_context,
                               client_id_taxopark, person_id_taxopark, taxi_contract_id,
                               amount=deposit_payout_second_month_2, invoice=lsd_external_id, dt=PREV_MONTH_START_DT)

    create_and_export_fake_cpf_insert_fuel_fact(lsz_external_id, lsz_id, fuel_fact_2, PREV_MONTH_START_DT)

    steps.PartnerSteps.get_partner_balance(zaxi_context.service, [zaxi_contract_id])

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id_taxopark, taxi_contract_id,
                                                                   PREV_MONTH_START_DT, manual_export=need_taxi_act)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id_taxopark, zaxi_contract_id,
                                                                   PREV_MONTH_START_DT)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id_gas_station, gas_station_contract_id,
                                                                   PREV_MONTH_START_DT)

    if need_taxi_act:
        taxi_total_act_sum = deposit_payout_first_month + deposit_payout_second_month_1 + \
                             completions_sum_1 + completions_sum_2 + fuel_fact + deposit_payout_second_month_2
    else:
        taxi_total_act_sum = D('0')

    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id_taxopark)
    expected_invoice_data = [
        steps.CommonData.create_expected_invoice_data_by_context(
            zaxi_context, zaxi_contract_id,
            person_id_taxopark,
            fuel_fact_1 + fuel_fact_2,
            dt=MONTH_BEFORE_PREV_START_DT,
            total_act_sum=completions_sum_1 + completions_sum_2 + fuel_fact,
            consume_sum=completions_sum_1 + completions_sum_2 + fuel_fact
        ),
        steps.CommonData.create_expected_invoice_data_by_context(
            taxi_context, taxi_contract_id,
            person_id_taxopark,
            deposit_payment,
            dt=MONTH_BEFORE_PREV_START_DT,
            paysys_id=taxi_context.additional_paysys.id,
            total_act_sum=taxi_total_act_sum,
            consume_sum=taxi_total_act_sum)
    ]
    utils.check_that(invoice_data, contains_dicts_equal_to(expected_invoice_data, same_length=False),
                     'Сравниваем данные из счета с шаблоном')

    expected_act_data = [
        steps.CommonData.create_expected_act_data(completions_sum_1, MONTH_BEFORE_PREV_END_DT),
        steps.CommonData.create_expected_act_data(completions_sum_2 + fuel_fact, PREV_MONTH_END_DT),
    ]

    if need_taxi_act:
        expected_act_data.extend([
            steps.CommonData.create_expected_act_data(deposit_payout_first_month + completions_sum_1,
                                                      MONTH_BEFORE_PREV_END_DT, type=ActType.INTERNAL),
            steps.CommonData.create_expected_act_data(deposit_payout_second_month_1 + deposit_payout_second_month_2 +
                                                      completions_sum_2 + fuel_fact, PREV_MONTH_END_DT, type=ActType.INTERNAL)
        ])

    act_data_first_month = steps.ActsSteps.get_act_data_by_client(client_id_taxopark, internal=True)
    utils.check_that(act_data_first_month, contains_dicts_with_entries(expected_act_data),
                     'Сравниваем данные из акта с шаблоном')

    gas_station_invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id_gas_station)
    expected_gas_station_invoice_data = [
        steps.CommonData.create_expected_invoice_data_by_context(
            gas_station_agent_context, gas_station_contract_id,
            person_id_gas_station,
            gas_station_reward_1 + gas_station_reward_2,
            dt=MONTH_BEFORE_PREV_START_DT,
            total_act_sum=gas_station_reward_1 + gas_station_reward_2,
            consume_sum=gas_station_reward_1 + gas_station_reward_2
        ),
    ]
    utils.check_that(gas_station_invoice_data, contains_dicts_equal_to(expected_gas_station_invoice_data, same_length=True),
                     'Сравниваем данные из счета с шаблоном')

    gas_station_act_data = steps.ActsSteps.get_act_data_by_client(client_id_gas_station)
    expected_gas_station_act_data = [
        steps.CommonData.create_expected_act_data(gas_station_reward_1, MONTH_BEFORE_PREV_END_DT),
        steps.CommonData.create_expected_act_data(gas_station_reward_2, PREV_MONTH_END_DT),
    ]

    utils.check_that(gas_station_act_data, contains_dicts_with_entries(expected_gas_station_act_data),
                     'Сравниваем данные из акта с шаблоном')


def create_contracts(taxi_context, zaxi_context, azs_payouts_context):
    client_id_taxopark, person_id_taxopark, taxi_contract_id, taxi_contract_eid = steps.ContractSteps. \
        create_partner_contract(taxi_context, is_postpay=0, additional_params={'start_dt': MONTH_BEFORE_PREV_START_DT})
    _, _, zaxi_contract_id, _ = steps.ContractSteps. \
        create_partner_contract(zaxi_context, is_postpay=0, client_id=client_id_taxopark, person_id=person_id_taxopark,
                                additional_params={'start_dt': MONTH_BEFORE_PREV_START_DT,
                                                   'link_contract_id': taxi_contract_id})

    if azs_payouts_context:
        if azs_payouts_context.contract_type == ContractSubtype.SPENDABLE:
            client_id_gas_station, person_id_gas_station, azs_contract_id, _ = steps.ContractSteps. \
                create_partner_contract(azs_payouts_context, is_offer=1,
                                        additional_params={'start_dt': MONTH_BEFORE_PREV_START_DT})
        else:
            client_id_gas_station, person_id_gas_station, azs_contract_id, _ = steps.ContractSteps. \
                create_partner_contract(azs_payouts_context, additional_params={'start_dt': MONTH_BEFORE_PREV_START_DT})
    else:
        client_id_gas_station, person_id_gas_station, azs_contract_id = [None]*3
    return client_id_taxopark, client_id_gas_station, person_id_taxopark, taxi_contract_id, taxi_contract_eid, \
           zaxi_contract_id, person_id_gas_station, azs_contract_id


def create_fake_deposit_payment(taxi_context, zaxi_context, gas_station_payout_context,
                                client_id_taxopark, person_id_taxopark, taxi_contract_id, amount, invoice, dt):
    simple_api_steps.SimpleApi.create_fake_tpt_row(taxi_context, client_id_taxopark, person_id_taxopark,
                                                   taxi_contract_id,
                                                   dt=dt,
                                                   transaction_type=TransactionType.REFUND,
                                                   amount=amount,
                                                   payment_type=PaymentType.DEPOSIT,
                                                   paysys_type_cc=PaysysType.FUEL_HOLD,
                                                   client_id=client_id_taxopark,
                                                   invoice_eid=invoice,
                                                   product_id=taxi_context.zaxi_deposit_product.id)


def create_fake_deposit_payout(taxi_context, zaxi_context, gas_station_payout_context,
                               client_id_taxopark, person_id_taxopark, taxi_contract_id, amount, invoice, dt):
    simple_api_steps.SimpleApi.create_fake_tpt_row(taxi_context, client_id_taxopark, person_id_taxopark,
                                                   taxi_contract_id,
                                                   dt=dt,
                                                   transaction_type=TransactionType.PAYMENT,
                                                   amount=amount,
                                                   payment_type=PaymentType.DEPOSIT_PAYOUT,
                                                   paysys_type_cc=PaysysType.FUEL_HOLD_PAYMENT,
                                                   client_id=client_id_taxopark,
                                                   invoice_eid=invoice,
                                                   product_id=taxi_context.zaxi_deposit_product.id,
                                                   yandex_reward=amount)


def create_fake_fuel_fact(taxi_context, zaxi_context, gas_station_payout_context,
                          client_id_taxopark, client_id_gas_station, person_id_taxopark,
                          person_id_fuel_station, taxi_contract_id, zaxi_contract_id, gas_station_contract_id,
                          amount, LSD, LSZ, dt, GAS_STATION_LS=None, gas_station_reward=None):
    simple_api_steps.SimpleApi.create_fake_tpt_row(zaxi_context, client_id_gas_station,
                                                   person_id_taxopark, zaxi_contract_id, dt=dt,
                                                   transaction_type=TransactionType.PAYMENT,
                                                   amount=amount,
                                                   payment_type=PaymentType.REFUEL,
                                                   paysys_type_cc=PaysysType.FUEL_FACT,
                                                   client_id=client_id_taxopark,
                                                   invoice_eid=LSZ,
                                                   yandex_reward=amount,
                                                   )

    simple_api_steps.SimpleApi.create_fake_tpt_row(taxi_context, client_id_gas_station,
                                                   person_id_taxopark, taxi_contract_id, dt=dt,
                                                   transaction_type=TransactionType.PAYMENT,
                                                   amount=amount,
                                                   payment_type=PaymentType.REFUEL,
                                                   paysys_type_cc=PaysysType.FUEL_FACT,
                                                   client_id=client_id_taxopark,
                                                   invoice_eid=LSD,
                                                   product_id=taxi_context.zaxi_deposit_product.id,
                                                   yandex_reward=amount,
                                                   internal=1)
    if gas_station_payout_context:
        if gas_station_payout_context.contract_type == ContractSubtype.SPENDABLE:
            simple_api_steps.SimpleApi.create_fake_tpt_row(gas_station_payout_context, client_id_gas_station,
                                                           person_id_fuel_station, gas_station_contract_id, dt=dt,
                                                           transaction_type=TransactionType.PAYMENT,
                                                           amount=amount,
                                                           payment_type=PaymentType.REFUEL,
                                                           paysys_type_cc=PaysysType.TAXI,
                                                           client_id=client_id_taxopark)
        else:
            simple_api_steps.SimpleApi.create_fake_tpt_row(gas_station_payout_context, client_id_gas_station,
                                                           person_id_fuel_station, gas_station_contract_id, dt=dt,
                                                           transaction_type=TransactionType.PAYMENT,
                                                           amount=amount,
                                                           yandex_reward=gas_station_reward,
                                                           payment_type=PaymentType.REFUEL,
                                                           paysys_type_cc=PaysysType.TAXI,
                                                           client_id=client_id_taxopark,
                                                           invoice_eid=GAS_STATION_LS)



def create_and_export_fake_cpf(invoice_eid, invoice_id, amount, dt, type):
    cash_fact_id, _ = steps.TaxiSteps.create_cash_payment_fact(invoice_eid, amount, dt, type)
    steps.CommonSteps.export(Export.Type.PROCESS_PAYMENTS, Export.Classname.INVOICE, invoice_id)
    return cash_fact_id


def create_and_export_fake_cpf_insert_fuel_hold(invoice_eid, invoice_id, amount, dt):
    type = OEBSOperationType.INSERT_FUEL_HOLD
    cash_fact_id = create_and_export_fake_cpf(invoice_eid, invoice_id, amount, dt, type)
    return cash_fact_id


def create_and_export_fake_cpf_insert_fuel_fact(invoice_eid, invoice_id, amount, dt):
    type = OEBSOperationType.INSERT_FUEL_FACT
    cash_fact_id = create_and_export_fake_cpf(invoice_eid, invoice_id, amount, dt, type)
    return cash_fact_id


def create_and_export_fake_cpf_insert_fuel_hold_payment(invoice_eid, invoice_id, amount, dt):
    type = OEBSOperationType.INSERT_FUEL_HOLD_RETURN
    cash_fact_id = create_and_export_fake_cpf(invoice_eid, invoice_id, amount, dt, type)
    return cash_fact_id


def balance_expected_data(taxi_context, zaxi_context, zaxi_spendable_context,
                          contract_id, contract_eid, deposit_payment, zaxi_sum, deposit_payout, invoice_eid,
                          is_act_generated=False):
    consume_sum = deposit_payout + zaxi_sum if is_act_generated else D('0')
    balance = deposit_payment - (deposit_payout + zaxi_sum)
    return [{'ActSum': consume_sum, 'Balance': balance, 'ConsumeSum': consume_sum, 'ContractID': contract_id,
             'Currency': zaxi_context.currency.iso_code, 'ReceiptSum': deposit_payment,
             'TotalCharge': deposit_payout + zaxi_sum,
             'ContractEID': contract_eid, 'InvoiceEID': invoice_eid}]
