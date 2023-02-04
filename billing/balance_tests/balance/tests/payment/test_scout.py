# -*- coding: utf-8 -*-

import datetime
from decimal import Decimal as D

import pytest

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import TransactionType, Export, PaymentType
from btestlib.data.partner_contexts import SCOUTS_RU_CONTEXT, TAXI_RU_CONTEXT, \
    SCOUTS_KZ_CONTEXT, TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT
from btestlib.matchers import contains_dicts_with_entries
from balance.features import AuditFeatures
import btestlib.reporter as reporter

payment_dt = datetime.datetime.now()
default_amount = D('1000.34')

payload = '{"db"="5055e1c619914eef979fef011fa5b912";"scout_id"="mvoronovubervrz";"scout_name"="uber \u0432\u043e' \
          '\u0440\u043e\u043d\u043e\u0432";"uuid"="5055e1c619914eef979fef011fa5b912_mvoronovubervrz"}'


def create_client_and_contract(context_scout, context_taxi):
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(context_taxi)

    _, spendable_person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(context_scout,
                                                                                                   client_id=client_id,
                                                                                                   is_offer=1,
                                                                                                   additional_params={
                                                                                                       'link_contract_id': contract_id})

    return client_id, spendable_person_id, spendable_contract_id


@pytest.mark.audit(reporter.feature(AuditFeatures.Taxi_Payments))
@pytest.mark.parametrize('payment_type, transaction_type, context_scout, context_taxi', [
    pytest.mark.smoke((PaymentType.SCOUT, TransactionType.PAYMENT, SCOUTS_RU_CONTEXT, TAXI_RU_CONTEXT)),
    (PaymentType.SCOUT_SZ, TransactionType.PAYMENT, SCOUTS_RU_CONTEXT, TAXI_RU_CONTEXT),
    (PaymentType.SCOUT_CARGO_SUBSIDY, TransactionType.PAYMENT, SCOUTS_RU_CONTEXT, TAXI_RU_CONTEXT),
    (PaymentType.SCOUT_CARGO_SZ_SUBSIDY, TransactionType.PAYMENT, SCOUTS_RU_CONTEXT, TAXI_RU_CONTEXT),
    (PaymentType.SCOUT, TransactionType.REFUND, SCOUTS_RU_CONTEXT, TAXI_RU_CONTEXT),
    (PaymentType.SCOUT_SZ, TransactionType.REFUND, SCOUTS_RU_CONTEXT, TAXI_RU_CONTEXT),
    (PaymentType.SCOUT_CARGO_SUBSIDY, TransactionType.REFUND, SCOUTS_RU_CONTEXT, TAXI_RU_CONTEXT),
    (PaymentType.SCOUT_CARGO_SZ_SUBSIDY, TransactionType.REFUND, SCOUTS_RU_CONTEXT, TAXI_RU_CONTEXT),
    # (PaymentType.SCOUT, TransactionType.PAYMENT, SCOUTS_KZ_CONTEXT, TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT),
    # (PaymentType.SCOUT_SZ, TransactionType.PAYMENT, SCOUTS_KZ_CONTEXT, TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT),
    # (PaymentType.SCOUT, TransactionType.REFUND, SCOUTS_KZ_CONTEXT, TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT),
    # (PaymentType.SCOUT_SZ, TransactionType.REFUND, SCOUTS_KZ_CONTEXT, TAXI_YA_TAXI_CORP_KZ_KZT_CONTEXT),
], ids=lambda p: p.name)
def test_scout(payment_type, transaction_type, context_scout, context_taxi):
    client_id, person_id, contract_id = create_client_and_contract(context_scout, context_taxi)

    side_payment_id, _ = steps.PartnerSteps.create_sidepayment_transaction(client_id, payment_dt, default_amount,
                                                                        payment_type, context_scout.service.id,
                                                                        payload=payload,
                                                                        currency=context_scout.currency,
                                                                        transaction_type=transaction_type)

    steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    thirdparty_transaction_id = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
            side_payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_id(thirdparty_transaction_id)

    trust_refund_id = payment_data[0]['trust_id'] if transaction_type == TransactionType.REFUND else None

    expected_data = [steps.SimpleApi.create_expected_tpt_row(context_scout, client_id, contract_id, person_id,
                                                            None, payment_data[0]['payment_id'],
                                                            trust_refund_id=trust_refund_id,
                                                            payment_type=payment_type, amount=default_amount,
                                                            trust_id=payment_data[0]['trust_id'])]
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')