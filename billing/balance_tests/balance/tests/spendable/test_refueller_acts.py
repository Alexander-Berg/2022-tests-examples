# -*- coding: utf-8 -*-
__author__ = 'yuelyasheva'

import datetime
from decimal import Decimal as D
import pytest

from balance import balance_steps as steps
from btestlib.constants import *
from btestlib.data.defaults import *
from btestlib.matchers import contains_dicts_with_entries
from btestlib.data.partner_contexts import REFUELLER_CONTEXT, REFUELLER_SPENDABLE_CONTEXT
from balance.features import Features
import btestlib.reporter as reporter


CONTRACT_START_DT, _, MONTH_BEFORE_PREV_START_DT, MONTH_BEFORE_PREV_END_DT, \
    PREV_MONTH_START_DT, PREV_MONTH_END_DT = utils.Date.previous_three_months_start_end_dates()
PAYMENT_DT = datetime.now()
AMOUNT = D('667.66')
AMOUNT_PENALTY = D('123.45')


@reporter.feature(Features.PARTNER_ACT, Features.PARTNER, Features.REFUELLER)
@pytest.mark.tickets('BALANCE-31248')
@pytest.mark.smoke
def test_act_two_months():
    client_id, _, penalty_contract_id, _ = steps.ContractSteps.create_partner_contract(
        REFUELLER_CONTEXT, additional_params={'start_dt': CONTRACT_START_DT})

    _, penalty_invoice_eid = steps.InvoiceSteps.get_invoice_ids(client_id)
    _, spendable_person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
        REFUELLER_SPENDABLE_CONTEXT, client_id=client_id,
        additional_params={'start_dt': CONTRACT_START_DT,
                           'link_contract_id': penalty_contract_id})

    # добавляем штрафную транзакцию чтобы проверить, что не учитываем ее в акте
    steps.SimpleApi.create_fake_tpt_data(REFUELLER_CONTEXT, client_id, spendable_person_id,
                                         spendable_contract_id, MONTH_BEFORE_PREV_START_DT,
                                         [{'amount': AMOUNT_PENALTY,
                                           'transaction_type': TransactionType.REFUND}])

    first_month_sum = create_completions(client_id, spendable_person_id, spendable_contract_id,
                                         MONTH_BEFORE_PREV_START_DT, penalty_invoice_eid)

    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, MONTH_BEFORE_PREV_END_DT)

    second_month_sum = create_completions(client_id, spendable_person_id, spendable_contract_id,
                                          MONTH_BEFORE_PREV_START_DT, penalty_invoice_eid, D('2'))
    second_month_sum += create_completions(client_id, spendable_person_id, spendable_contract_id,
                                           PREV_MONTH_START_DT, penalty_invoice_eid, D('3'))

    steps.CommonPartnerSteps.generate_partner_acts_fair(spendable_contract_id, PREV_MONTH_END_DT)
    act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(spendable_contract_id)
    expected_act_data = [
        steps.CommonData.create_expected_pad(REFUELLER_SPENDABLE_CONTEXT, client_id,
                                             spendable_contract_id, MONTH_BEFORE_PREV_END_DT,
                                             partner_reward=first_month_sum,
                                             nds=REFUELLER_SPENDABLE_CONTEXT.nds),
        steps.CommonData.create_expected_pad(REFUELLER_SPENDABLE_CONTEXT, client_id,
                                             spendable_contract_id, PREV_MONTH_END_DT,
                                             partner_reward=second_month_sum,
                                             nds=REFUELLER_SPENDABLE_CONTEXT.nds)]

    utils.check_that(act_data, contains_dicts_with_entries(expected_act_data), u'Сравниваем данные из акта с шаблоном')


def create_completions(client_id, person_id, contract_id, dt, penalty_invoice_eid, coef=1):
    steps.SimpleApi.create_fake_tpt_data(REFUELLER_SPENDABLE_CONTEXT, client_id, person_id,
                                         contract_id, dt,
                                         [{'amount': coef * AMOUNT,
                                           'transaction_type': TransactionType.PAYMENT,
                                           'invoice_eid': penalty_invoice_eid,
                                           'total_sum':  coef * AMOUNT},
                                          {'amount': coef * AMOUNT / D('2'),
                                           'transaction_type': TransactionType.REFUND,
                                           'invoice_eid': penalty_invoice_eid,
                                           'total_sum': coef * AMOUNT / D('2')}])
    return coef * D('0.5') * AMOUNT
