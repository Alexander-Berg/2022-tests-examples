# -*- coding: utf-8 -*-

from decimal import Decimal as D
import pytest
from balance import balance_steps as steps
from btestlib.constants import *
from btestlib.data.defaults import *
from btestlib.matchers import contains_dicts_with_entries
from btestlib.data.partner_contexts import REFUELLER_CONTEXT, REFUELLER_SPENDABLE_CONTEXT
from balance.features import Features
import btestlib.reporter as reporter


PAYMENT_DT = datetime.now()
AMOUNT = D('667.66')


# штрафные транзакции только рефанды!
@reporter.feature(Features.PAYMENT, Features.PARTNER, Features.REFUELLER)
@pytest.mark.tickets('BALANCE-31247')
def test_refueller_penalty_refund():
    client_id, _, penalty_contract_id, _ = steps.ContractSteps.create_partner_contract(REFUELLER_CONTEXT)
    penalty_invoice_id, penalty_invoice_eid = steps.InvoiceSteps.get_invoice_ids(client_id)

    _, spendable_person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
        REFUELLER_SPENDABLE_CONTEXT, client_id=client_id,
        additional_params={'link_contract_id': penalty_contract_id})

    side_payment_id, _ = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, PAYMENT_DT, AMOUNT,
                                                          PaymentType.CARD,
                                                          REFUELLER_CONTEXT.service.id,
                                                          transaction_type=TransactionType.REFUND)

    steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
	                                                      Export.Classname.SIDE_PAYMENT)
    thirdparty_transaction_id = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
        side_payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_id(thirdparty_transaction_id)
    expected_data = [steps.SimpleApi.create_expected_tpt_row(REFUELLER_CONTEXT, client_id,
                                                             spendable_contract_id, spendable_person_id,
                                                             None, payment_data[0]['payment_id'],
                                                             amount=AMOUNT,
                                                             trust_id=payment_data[0]['trust_id'],
                                                             invoice_eid=penalty_invoice_eid,
                                                             transaction_type=TransactionType.REFUND.name
                                                             )]
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')


@reporter.feature(Features.PAYMENT, Features.PARTNER, Features.REFUELLER)
@pytest.mark.tickets('BALANCE-31247')
@pytest.mark.smoke
def test_refueller_spendable_payment():
    client_id, _, penalty_contract_id, _ = steps.ContractSteps.create_partner_contract(REFUELLER_CONTEXT)

    _, spendable_person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
        REFUELLER_SPENDABLE_CONTEXT, client_id=client_id,
        additional_params={'link_contract_id': penalty_contract_id})

    payment_type = REFUELLER_SPENDABLE_CONTEXT.tpt_payment_type
    side_payment_id, _ = steps.PartnerSteps.create_sidepayment_transaction(client_id, PAYMENT_DT, AMOUNT,
                                                                           payment_type,
                                                                           REFUELLER_SPENDABLE_CONTEXT.service.id)

    steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
	                                                      Export.Classname.SIDE_PAYMENT)
    thirdparty_transaction_id = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
        side_payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_id(thirdparty_transaction_id)

    expected_data = [steps.SimpleApi.create_expected_tpt_row(REFUELLER_SPENDABLE_CONTEXT, client_id,
                                                             spendable_contract_id, spendable_person_id,
                                                             None, payment_data[0]['payment_id'],
                                                             payment_type=payment_type, amount=AMOUNT,
                                                             trust_id=payment_data[0]['trust_id']
                                                             )]
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')


@reporter.feature(Features.REFUND, Features.PARTNER, Features.REFUELLER)
@pytest.mark.tickets('BALANCE-31247')
def test_refueller_spendable_refund():
    client_id, _, penalty_contract_id, _ = steps.ContractSteps.create_partner_contract(REFUELLER_CONTEXT)

    _, spendable_person_id, spendable_contract_id, _ = steps.ContractSteps.create_partner_contract(
        REFUELLER_SPENDABLE_CONTEXT, client_id=client_id,
        additional_params={'link_contract_id': penalty_contract_id})

    payment_type = REFUELLER_SPENDABLE_CONTEXT.tpt_payment_type
    side_payment_id, side_transaction_id = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, PAYMENT_DT, AMOUNT, payment_type,
                                                          REFUELLER_SPENDABLE_CONTEXT.service.id)

    side_payment_id_refund, _ = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, PAYMENT_DT, AMOUNT/D('2'), payment_type,
                                                          REFUELLER_SPENDABLE_CONTEXT.service.id,
                                                          transaction_type=TransactionType.REFUND,
                                                          orig_transaction_id=side_transaction_id)

    steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
	                                                      Export.Classname.SIDE_PAYMENT)
    steps.ExportSteps.create_export_record_and_export(side_payment_id_refund, Export.Type.THIRDPARTY_TRANS,
	                                                      Export.Classname.SIDE_PAYMENT, with_export_record=False)

    thirdparty_transaction_id_refund = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
        side_payment_id_refund)
    refund_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_id(thirdparty_transaction_id_refund)

    expected_data_refund = \
        [steps.SimpleApi.create_expected_tpt_row(REFUELLER_SPENDABLE_CONTEXT, client_id,
                                                 spendable_contract_id, spendable_person_id,
                                                 None, refund_data[0]['payment_id'],
                                                 payment_type=payment_type, amount=AMOUNT/D('2'),
                                                 trust_id=refund_data[0]['trust_id'],
                                                 transaction_type=TransactionType.REFUND.name)]
    utils.check_that(refund_data, contains_dicts_with_entries(expected_data_refund), u'Сравниваем рефанд с шаблоном')
