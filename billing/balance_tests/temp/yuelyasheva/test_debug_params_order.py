# coding: utf-8


from decimal import Decimal

import pytest

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import TransactionType, Export, PaymentType, PaysysType
from btestlib.data.partner_contexts import FOOD_COURIER_SPENDABLE_CONTEXT, FOOD_COURIER_SPENDABLE_KZ_CONTEXT, \
    FOOD_COURIER_SPENDABLE_BY_CONTEXT, LAVKA_COURIER_SPENDABLE_CONTEXT
from btestlib.matchers import contains_dicts_with_entries

CONTEXTS = [
    FOOD_COURIER_SPENDABLE_CONTEXT,
    # FOOD_COURIER_SPENDABLE_KZ_CONTEXT,
    # FOOD_COURIER_SPENDABLE_BY_CONTEXT,
    LAVKA_COURIER_SPENDABLE_CONTEXT,
]

PAYMENT_TYPES = [
    PaymentType.SUBSIDY,
    # PaymentType.COUPON,
    # PaymentType.CORP_COUPON,
    # PaymentType.CORP_SUBSIDY
]
START_DT = utils.Date.first_day_of_month()
AMOUNT = Decimal('100.11')


@pytest.mark.parametrize("context", CONTEXTS, ids=lambda c: c.name)
@pytest.mark.parametrize("payment_type", PAYMENT_TYPES, ids=lambda pt: pt.upper())
@pytest.mark.parametrize("transaction_type", TransactionType.values(), ids=lambda tt: tt.name.upper())
def test_food_payment_subsidy(context, payment_type, transaction_type):
    client_id, person_id, contract_id, _ = \
        steps.ContractSteps.create_partner_contract(context, additional_params={
            'services': None
        })
