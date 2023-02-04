from decimal import Decimal as D

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import Nds
from btestlib.data.partner_contexts import CORP_TAXI_KZ_CONTEXT_SPENDABLE
from btestlib.matchers import contains_dicts_equal_to
from decimal import Decimal

first_month_start_dt, first_month_end_dt, second_month_start_dt, second_month_end_dt, _, _ = \
    utils.Date.previous_three_months_start_end_dates()


def create_client_persons_contracts(context):
    client_id, person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
            context,
            additional_params={
                'start_dt': first_month_start_dt,
            #    'ctype': 'SPENDABLE'
            })
    return client_id, spendable_contract_id, person_id

#Nds.DEFAULT, True, BUSES_DONATE_RU_CONTEXT
context = CORP_TAXI_KZ_CONTEXT_SPENDABLE
client_id, contract_id, person_id = create_client_persons_contracts(context)