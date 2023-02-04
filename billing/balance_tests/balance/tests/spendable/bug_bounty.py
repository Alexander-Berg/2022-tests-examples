# -*- coding: utf-8 -*-
from decimal import Decimal

import balance.balance_api as api

from datetime import datetime
from btestlib import utils
from btestlib.matchers import contains_dicts_with_entries
from btestlib.constants import Currencies, PersonTypes, NdsNew

from balance import balance_steps as steps
from btestlib.data.partner_contexts import BUG_BOUNTY_CONTEXT

import pytest

PREV_MONTH_START_DT, PREV_MONTH_END_DT = utils.Date.previous_month_first_and_last_days(datetime.now())
COMPL_SUM = Decimal('100000.2')


@pytest.mark.parametrize('person_type, currency',
                         [(PersonTypes.PH, Currencies.RUB), (PersonTypes.YTPH, Currencies.USD)],
                         ids=['ph', 'ytph']
                         )
def test_act(person_type, currency):

    context = BUG_BOUNTY_CONTEXT.new(person_type=person_type, currency=currency)

    client_id, person_id, contract_id, _ = (
        steps.ContractSteps.create_partner_contract(context, additional_params={'start_dt': PREV_MONTH_START_DT}, is_offer=True)
    )
    # Добавляем открутки
    eid = steps.tarification_entity_steps.get_tarification_entity_id(product_id=context.page_id,
                                                                     key_num_1=client_id,
                                                                     key_num_2=context.currency.iso_num_code)

    steps.partner_steps.PartnerSteps.create_entity_completions_row(
        context.page_id, eid, context.source_id, COMPL_SUM, PREV_MONTH_START_DT)

    # Запускаем калькулятор
    api.test_balance().RunPartnerCalculator(contract_id, PREV_MONTH_START_DT)

    # Генерим акты
    api.test_balance().GeneratePartnerAct(contract_id, PREV_MONTH_END_DT)

    partner_act_data = steps.CommonPartnerSteps.get_partner_act_data_by_contract_id(contract_id)

    expected_partner_act_data = [
        steps.common_data_steps.CommonData.create_expected_pad(
            context, client_id, contract_id, PREV_MONTH_END_DT, COMPL_SUM, nds=NdsNew.ZERO)
    ]

    utils.check_that(partner_act_data, contains_dicts_with_entries(expected_partner_act_data),
                     u"Проверяем, что сгенерился акт с ожидаемыми параметрами")
