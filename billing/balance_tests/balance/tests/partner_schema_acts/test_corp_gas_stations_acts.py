# -*- coding: utf-8 -*-

from decimal import Decimal as D
import pytest
from hamcrest import empty, equal_to
import balance.balance_api as api

from balance import balance_steps as steps
from btestlib import utils

from btestlib.constants import Services, TransactionType, PaymentType, Export, PaysysType, ServiceCode
from balance.balance_steps import simple_api_steps
from btestlib.matchers import contains_dicts_with_entries, contains_dicts_equal_to
from btestlib.data.partner_contexts import ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, ZAXI_DELIVERY_RU_CONTEXT

_, _, MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, PREV_MONTH_START_DT, PREV_MONTH_END_DT = \
    utils.Date.previous_three_months_start_end_dates()
CURRENT_MONTH_START_DT, _ = utils.Date.current_month_first_and_last_days()

COMPLETION_SUM = D('11.22')
CHARGE_AMOUNT = D('100')


@pytest.mark.tickets('BALANCE-30770')
@pytest.mark.parametrize('is_postpay', [
        pytest.param(0, id='prepay'),
        pytest.param(1, id='postpay'),
])
@pytest.mark.parametrize('general_context, spendable_context', [
    pytest.param(ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_CORP_RU'),
    pytest.param(ZAXI_DELIVERY_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_DELIVERY_CORP_RU'),
])
def test_corp_gas_stations_wo_data(is_postpay, general_context, spendable_context):
    client_id, person_id, contract_id, _, client_id_gas_station, \
        person_id_gas_station, contract_id_gas_station, _ = create_contracts(is_postpay, general_context, spendable_context)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                   MONTH_BEFORE_PREV_START_DT, manual_export=False)
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    utils.check_that(act_data, empty(), 'Сравниваем данные из акта с шаблоном')

    if spendable_context:
        steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id_gas_station, MONTH_BEFORE_PREV_START_DT)
        spendable_acts = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id_gas_station)
        utils.check_that(spendable_acts, empty(), 'Сравниваем данные из акта с шаблоном')


@pytest.mark.tickets('BALANCE-30770')
@pytest.mark.parametrize('is_postpay, payment', [
    pytest.param(0, 0, id='prepay, no payments'),
    pytest.param(0, 1, id='prepay, payment'),
    pytest.param(1, 0, id='postpay'),
])
@pytest.mark.parametrize('general_context, spendable_context', [
    pytest.param(ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_CORP_RU'),
    pytest.param(ZAXI_DELIVERY_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_DELIVERY_CORP_RU'),
])
def test_corp_gas_stations_increasing_total(is_postpay, payment, general_context, spendable_context):
    (client_id, person_id, contract_id, _, client_id_gas_station,
     person_id_gas_station, contract_id_gas_station, _) = create_contracts(is_postpay, general_context, spendable_context)

    lsz_id, lsz_external_id, _ = steps.InvoiceSteps.get_invoice_by_service_or_service_code(contract_id,
                                                                                           Services.ZAXI,
                                                                                           ServiceCode.YANDEX_SERVICE)

    create_fake_fuel_fact(general_context, spendable_context,
                          client_id, client_id_gas_station, person_id, person_id_gas_station,
                          contract_id, contract_id_gas_station, lsz_external_id,
                          MONTH_BEFORE_PREV_START_DT, COMPLETION_SUM)
    create_fake_fuel_fact(general_context, spendable_context,
                          client_id, client_id_gas_station, person_id, person_id_gas_station,
                          contract_id, contract_id_gas_station, lsz_external_id,
                          MONTH_BEFORE_PREV_START_DT, COMPLETION_SUM / D('2'), is_refund=True)

    if payment:
        steps.InvoiceSteps.create_cash_payment_fact(lsz_external_id, CHARGE_AMOUNT, MONTH_BEFORE_PREV_START_DT,
                                                    'INSERT', invoice_id=lsz_id)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, MONTH_BEFORE_PREV_START_DT)
    if spendable_context:
        steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id_gas_station, MONTH_BEFORE_PREV_START_DT)

    create_fake_fuel_fact(general_context, spendable_context,
                          client_id, client_id_gas_station, person_id, person_id_gas_station,
                          contract_id, contract_id_gas_station, lsz_external_id,
                          MONTH_BEFORE_PREV_START_DT, COMPLETION_SUM / D('2'))

    create_fake_fuel_fact(general_context, spendable_context,
                          client_id, client_id_gas_station, person_id, person_id_gas_station,
                          contract_id, contract_id_gas_station, lsz_external_id, PREV_MONTH_START_DT,
                          COMPLETION_SUM * D('2'))
    create_fake_fuel_fact(general_context, spendable_context,
                          client_id, client_id_gas_station, person_id, person_id_gas_station,
                          contract_id, contract_id_gas_station, lsz_external_id,
                          PREV_MONTH_START_DT, COMPLETION_SUM / D('2'), is_refund=True)

    if payment:
        steps.InvoiceSteps.create_cash_payment_fact(lsz_external_id, CHARGE_AMOUNT, MONTH_BEFORE_PREV_START_DT,
                                                    'INSERT', invoice_id=lsz_id)

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id, PREV_MONTH_START_DT)
    if spendable_context:
        steps.CommonPartnerSteps.generate_partner_acts_fair(contract_id_gas_station, PREV_MONTH_START_DT)

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)

    expected_act_data = [
        steps.CommonData.create_expected_act_data(COMPLETION_SUM / D('2'), MONTH_BEFORE_PREV_END_DT),
        steps.CommonData.create_expected_act_data(COMPLETION_SUM * D('2'), PREV_MONTH_END_DT)
    ]
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data), 'Сравниваем данные из акта с шаблоном')

    if spendable_context:
        spendable_acts = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id_gas_station)
        utils.check_that(spendable_acts, empty(), 'Сравниваем данные из акта с шаблоном')


@pytest.mark.tickets('BALANCE-30770')
@pytest.mark.parametrize('is_postpay', [
        pytest.param(0, id='prepay'),
        pytest.param(1, id='postpay'),
])
@pytest.mark.parametrize('general_context, spendable_context', [
    pytest.param(ZAXI_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_CORP_RU'),
    pytest.param(ZAXI_DELIVERY_RU_CONTEXT, ZAXI_RU_SPENDABLE_CONTEXT, id='ZAXI_DELIVERY_CORP_RU'),
])
def test_corp_gas_stations_balance(is_postpay, general_context, spendable_context):
    (client_id, person_id, contract_id, contract_eid, client_id_gas_station,
     person_id_gas_station, contract_id_gas_station, _) = create_contracts(is_postpay, general_context, spendable_context)

    lsz_id, lsz_external_id, _ = steps.InvoiceSteps.get_invoice_by_service_or_service_code(contract_id,
                                                                                           general_context.service,
                                                                                           ServiceCode.YANDEX_SERVICE)

    balance = steps.PartnerSteps.get_partner_balance(general_context.service, [contract_id])
    expected_balance = get_expected_balance_data(general_context, D('0'), D('0'), contract_id)
    utils.check_that(balance, contains_dicts_with_entries(expected_balance),
                     'Сравниваем балансы после создания договора')

    steps.InvoiceSteps.create_cash_payment_fact(lsz_external_id, CHARGE_AMOUNT, PREV_MONTH_START_DT, 'INSERT',
                                                invoice_id=lsz_id)

    balance = steps.PartnerSteps.get_partner_balance(general_context.service, [contract_id])
    expected_balance = get_expected_balance_data(general_context, D('0'), CHARGE_AMOUNT, contract_id)
    utils.check_that(balance, contains_dicts_with_entries(expected_balance),
                     'Сравниваем балансы после оплаты квитанции')

    create_fake_fuel_fact(general_context, spendable_context,
                          client_id, client_id_gas_station, person_id, person_id_gas_station,
                          contract_id, contract_id_gas_station, lsz_external_id, PREV_MONTH_START_DT,
                          D('2') * COMPLETION_SUM)

    balance = steps.PartnerSteps.get_partner_balance(general_context.service, [contract_id])
    expected_balance = get_expected_balance_data(general_context, D('2') * COMPLETION_SUM, CHARGE_AMOUNT, contract_id)
    utils.check_that(balance, contains_dicts_with_entries(expected_balance),
                     'Сравниваем балансы после откруток (платежи)')

    create_fake_fuel_fact(general_context, spendable_context,
                          client_id, client_id_gas_station, person_id, person_id_gas_station,
                          contract_id, contract_id_gas_station, lsz_external_id, PREV_MONTH_START_DT,
                          COMPLETION_SUM, is_refund=True)

    balance = steps.PartnerSteps.get_partner_balance(general_context.service, [contract_id])
    expected_balance = get_expected_balance_data(general_context, COMPLETION_SUM, CHARGE_AMOUNT, contract_id)
    utils.check_that(balance, contains_dicts_with_entries(expected_balance),
                     'Сравниваем балансы после откруток (рефанды)')

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, contract_id,
                                                                   PREV_MONTH_START_DT)
    balance = steps.PartnerSteps.get_partner_balance(general_context.service, [contract_id])
    expected_balance = get_expected_balance_data(general_context, COMPLETION_SUM, CHARGE_AMOUNT, contract_id,
                                                 is_after_act=True)
    utils.check_that(balance, contains_dicts_with_entries(expected_balance),
                     'Сравниваем балансы после закрытия')

    steps.CommonPartnerSteps.export_partner_fast_balance(contract_id)

    expected_fast_balance = get_expected_fast_balance_data(general_context, CHARGE_AMOUNT, contract_id,
                                                           contract_eid, lsz_external_id)
    fast_balance_object = api.test_balance().GetPartnerFastBalance(contract_id, 'Contract', 'partner-fast-balance-zaxi')
    fast_balance_object['balance_info'].pop('DT')

    utils.check_that([fast_balance_object['balance_info']], contains_dicts_equal_to(expected_fast_balance),
                     'Сравниваем балансы c типом Export.Classname.CONTRACT')


def create_contracts(is_postpay, general_context, spendable_context):
    client_id, person_id, contract_id, contract_eid = \
        steps.ContractSteps.create_partner_contract(general_context, is_postpay=is_postpay, is_offer=1,
                                                    additional_params={'start_dt': MONTH_BEFORE_PREV_START_DT})
    if spendable_context:
        client_id_gas_station, person_id_gas_station, contract_id_gas_station, contract_eid_gas_station = \
            steps.ContractSteps.create_partner_contract(spendable_context, is_offer=1,
                                                        additional_params={'start_dt': MONTH_BEFORE_PREV_START_DT})
    else:
        client_id_gas_station, person_id_gas_station, contract_id_gas_station, contract_eid_gas_station = [None]*4
    return client_id, person_id, contract_id, contract_eid, client_id_gas_station, \
           person_id_gas_station, contract_id_gas_station, contract_id_gas_station


def create_fake_fuel_fact(general_context, spendable_context,
                          client_id, client_id_gas_station, person_id, person_id_gas_station,
                          contract_id, contract_id_gas_station, lsz_external_id, dt, amount, is_refund=False):

    simple_api_steps.SimpleApi.create_fake_tpt_row(general_context, client_id_gas_station,
                                                   person_id, contract_id, dt=dt,
                                                   transaction_type=TransactionType.PAYMENT if not is_refund
                                                                    else TransactionType.REFUND,
                                                   amount=amount,
                                                   payment_type=PaymentType.REFUEL,
                                                   paysys_type_cc=PaysysType.FUEL_FACT,
                                                   client_id=client_id,
                                                   invoice_eid=lsz_external_id,
                                                   yandex_reward=amount)
    if spendable_context:
        simple_api_steps.SimpleApi.create_fake_tpt_row(spendable_context, client_id_gas_station,
                                                       person_id_gas_station, contract_id_gas_station, dt=dt,
                                                       transaction_type=TransactionType.PAYMENT if not is_refund
                                                                        else TransactionType.REFUND,
                                                       amount=amount,
                                                       payment_type=PaymentType.REFUEL,
                                                       paysys_type_cc=PaysysType.TAXI,
                                                       client_id=client_id)


def get_expected_balance_data(general_context, act_sum, receipt_sum, contract_id, is_after_act=False):
    return [{'ActSum': act_sum if is_after_act else D('0'),
             'Balance': receipt_sum - act_sum,
             'TotalCharge': act_sum,
             'ConsumeSum': act_sum if is_after_act else D('0'),
             'ContractID': contract_id,
             'Currency': general_context.currency.iso_code,
             'ReceiptSum': receipt_sum}]


def get_expected_fast_balance_data(general_context, receipt_sum, contract_id, contract_eid, invoice_eid):
    return [{'ContractID': contract_id,
             'Currency': general_context.currency.iso_code,
             'ReceiptSum': receipt_sum,
             'InvoiceEID': invoice_eid,
             'ContractEID': contract_eid}]
