# coding: utf-8

__author__ = 'a-vasin'

from decimal import Decimal

import pytest

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import TransactionType, Export, PaymentType, PaysysType, Services
from btestlib.data.partner_contexts import FOOD_COURIER_SPENDABLE_CONTEXT, FOOD_COURIER_SPENDABLE_KZ_CONTEXT, \
    FOOD_COURIER_SPENDABLE_BY_CONTEXT, LAVKA_COURIER_SPENDABLE_CONTEXT, LAVKA_COURIER_SPENDABLE_ISR_CONTEXT
from btestlib.matchers import contains_dicts_with_entries


# Контексты и пеймент тайпы после проверки предлагается комментить кроме одного,
# чтобы не плодить одинаковые кейсы

CONTEXTS = [
    FOOD_COURIER_SPENDABLE_CONTEXT,
    # FOOD_COURIER_SPENDABLE_KZ_CONTEXT,
    # FOOD_COURIER_SPENDABLE_BY_CONTEXT,
    LAVKA_COURIER_SPENDABLE_CONTEXT,
    LAVKA_COURIER_SPENDABLE_ISR_CONTEXT
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
            'start_dt': START_DT
        })

    service_order_id = steps.CommonPartnerSteps.get_fake_trust_payment_id()
    transaction_id = steps.CommonPartnerSteps.get_fake_food_transaction_id()

    side_payment_id, side_transaction_id = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, START_DT, AMOUNT,
                                                          payment_type, context.service.id,
                                                          paysys_type_cc=PaysysType.YAEDA,
                                                          currency=context.currency,
                                                          extra_str_1=service_order_id,
                                                          transaction_id=transaction_id,
                                                          transaction_type=transaction_type,
                                                          payload="[]")

    steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    expected_transaction = create_expected_transaction(context, client_id, person_id, contract_id, transaction_type,
                                                       payment_type, transaction_id, side_payment_id, service_order_id)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_sidepayment_id(int(side_payment_id))
    utils.check_that(payment_data, contains_dicts_with_entries([expected_transaction]),
                     u'Сравниваем платеж с шаблоном')


# -------------------------------------------------------------------------------------
# Utils
def create_expected_transaction(context, client_id, person_id, contract_id, transaction_type, payment_type,
                                transaction_id, side_payment_id, service_order_id):
    tpt_attrs = {
        'transaction_type': transaction_type.name,
        'payment_type': payment_type,
        'amount': AMOUNT,

        # если субсидии, что в paysys_type_cc подставляем payment_type
        # https://st.yandex-team.ru/BALANCE-31502
        'paysys_type_cc': (
            payment_type if context.service == Services.FOOD_COURIER_SUBSIDY else context.tpt_paysys_type_cc),
        'dt': START_DT,
        'transaction_dt': START_DT,
        'trust_id': transaction_id,
        'service_order_id_str': service_order_id,
    }

    trust_payment_id = transaction_id if transaction_type == TransactionType.PAYMENT else None

    return steps.SimpleApi.create_expected_tpt_row(context, client_id, contract_id, person_id, trust_payment_id,
                                                   side_payment_id, trust=False, **tpt_attrs)
