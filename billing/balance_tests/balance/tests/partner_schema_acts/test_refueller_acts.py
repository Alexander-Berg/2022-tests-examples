# -*- coding: utf-8 -*-
__author__ = 'yuelyasheva'

from decimal import Decimal as D
import pytest

from balance import balance_steps as steps
from btestlib.constants import *
from btestlib.data.defaults import *
from btestlib.matchers import contains_dicts_with_entries, equal_to_casted_dict
from btestlib.data.partner_contexts import REFUELLER_CONTEXT, REFUELLER_SPENDABLE_CONTEXT
from hamcrest import empty
from balance.features import Features
import btestlib.reporter as reporter

CONTRACT_START_DT, _, MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, \
PREV_MONTH_START_DT, PREV_MONTH_END_DT = utils.Date.previous_three_months_start_end_dates()
PAYMENT_DT = datetime.now()
AMOUNT_1 = D('667.66')
AMOUNT_2 = D('123.45')


@reporter.feature(Features.ACT, Features.PARTNER, Features.REFUELLER)
@pytest.mark.tickets('BALANCE-31248')
def test_act_wo_data():
    client_id, _, penalty_contract_id, _ = steps.ContractSteps.create_partner_contract(
        REFUELLER_CONTEXT, additional_params={'start_dt': CONTRACT_START_DT})
    _, spendable_person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
        REFUELLER_SPENDABLE_CONTEXT, client_id=client_id,
        additional_params={'start_dt': CONTRACT_START_DT,
                           'link_contract_id': penalty_contract_id})

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(
        client_id, penalty_contract_id,
        MONTH_BEFORE_PREV_END_DT,
        manual_export=False)
    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    utils.check_that(act_data, empty(), step=u'Проверим, что акт не сгенерировался')


@reporter.feature(Features.ACT, Features.INVOICE, Features.PARTNER, Features.REFUELLER)
@pytest.mark.tickets('BALANCE-31248', 'BALANCE-31164')
@pytest.mark.smoke
def test_act_two_months():
    client_id, penalty_person_id, penalty_contract_id, _ = steps.ContractSteps.create_partner_contract(
        REFUELLER_CONTEXT, additional_params={'start_dt': CONTRACT_START_DT})

    _, penalty_invoice_eid = steps.InvoiceSteps.get_invoice_ids(client_id)
    _, spendable_person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
        REFUELLER_SPENDABLE_CONTEXT, client_id=client_id,
        additional_params={'start_dt': CONTRACT_START_DT,
                           'link_contract_id': penalty_contract_id})

    steps.SimpleApi.create_fake_tpt_data(REFUELLER_CONTEXT, client_id, spendable_person_id,
                                         spendable_contract_id, MONTH_BEFORE_PREV_START_DT,
                                         [{'amount': AMOUNT_1,
                                           'transaction_type': TransactionType.REFUND}])
    steps.SimpleApi.create_fake_tpt_data(REFUELLER_SPENDABLE_CONTEXT, client_id, spendable_person_id,
                                         spendable_contract_id, MONTH_BEFORE_PREV_START_DT,
                                         [{'amount': AMOUNT_2,
                                           'invoice_eid': penalty_invoice_eid,
                                           'total_sum': AMOUNT_2,
                                           'transaction_type': TransactionType.PAYMENT}])

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, penalty_contract_id,
                                                                   MONTH_BEFORE_PREV_END_DT)

    steps.SimpleApi.create_fake_tpt_data(REFUELLER_CONTEXT, client_id, spendable_person_id,
                                         spendable_contract_id, MONTH_BEFORE_PREV_START_DT,
                                         [{'amount': AMOUNT_1 / D('2'),
                                           'transaction_type': TransactionType.REFUND}])
    steps.SimpleApi.create_fake_tpt_data(REFUELLER_CONTEXT, client_id, spendable_person_id,
                                         spendable_contract_id, PREV_MONTH_START_DT,
                                         [{'amount': AMOUNT_2,
                                           'transaction_type': TransactionType.REFUND}])

    steps.CommonPartnerSteps.generate_partner_acts_fair_and_export(client_id, penalty_contract_id,
                                                                   PREV_MONTH_END_DT)
    amount = AMOUNT_1 * D('1.5') + AMOUNT_2
    # НДС в инвойсе 0, т.к. в t_invoice NDS=0. Это не баг, т.к. на НДС в ЛС мы не смотрим, все на стороне оебс.
    expected_invoice_data = steps.CommonData.create_expected_invoice_data_by_context(
        REFUELLER_CONTEXT,
        penalty_contract_id,
        penalty_person_id,
        amount,
        total_act_sum=amount,
        dt=PREV_MONTH_END_DT,
        nds=0)
    invoice_data = steps.InvoiceSteps.get_invoice_data_by_client(client_id)[0]
    utils.check_that(invoice_data, equal_to_casted_dict(expected_invoice_data),
                     u'Сравниваем данные из счета с шаблоном')

    act_data = steps.ActsSteps.get_act_data_by_client(client_id)
    expected_act_data = [steps.CommonData.create_expected_act_data(AMOUNT_1, MONTH_BEFORE_PREV_END_DT),
                         steps.CommonData.create_expected_act_data(AMOUNT_1 / D('2') + AMOUNT_2, PREV_MONTH_END_DT)]
    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data), u'Сравниваем данные из акта с шаблоном')
