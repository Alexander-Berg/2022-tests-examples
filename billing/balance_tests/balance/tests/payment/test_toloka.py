# -*- coding: utf-8 -*-
__author__ = 'a-vasin'

from decimal import Decimal

import pytest
from dateutil.relativedelta import relativedelta

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import Services, PaysysType, Export, PartnerPaymentType
from btestlib.data.partner_contexts import TOLOKA_CONTEXT
from btestlib.matchers import contains_dicts_with_entries

START_DT = utils.Date.first_day_of_month() - relativedelta(months=2)
AMOUNT = Decimal('1000.1')


@pytest.mark.smoke
def test_toloka_export_transaction():
    previous_month = utils.Date.first_day_of_month() - relativedelta(months=1)
    current_month = utils.Date.first_day_of_month()

    payment_data = [(START_DT, AMOUNT, PartnerPaymentType.WALLET),
                    (previous_month, AMOUNT * 2, PartnerPaymentType.EMAIL),
                    (current_month, AMOUNT * 3, PartnerPaymentType.WALLET)]

    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(TOLOKA_CONTEXT, is_offer=1,
                                                                                       additional_params={
                                                                                           'start_dt': START_DT})

    actual_payments = []
    expected_payments = []
    for dt, amount, payment_type in payment_data:
        payment_id, _ = steps.PartnerSteps.create_sidepayment_transaction(client_id, dt, amount, payment_type,
                                                                       Services.TOLOKA.id)
        steps.ExportSteps.create_export_record_and_export(payment_id, Export.Type.THIRDPARTY_TRANS,
                                                          Export.Classname.BALALAYKA_PAYMENT)

        thirdparty_transaction_id = steps.CommonPartnerSteps.get_synthetic_thirdparty_transaction_id_by_payment_id(
                payment_id)
        payment_data = steps.CommonPartnerSteps.get_thirdparty_payment_by_id(thirdparty_transaction_id)

        actual_payments.extend(payment_data)

        expected_payments.append(
            steps.SimpleApi.create_expected_tpt_row(TOLOKA_CONTEXT, client_id, contract_id, person_id,
                                                    None, payment_id, trust_id=None,
                                                    amount=utils.dround(amount / Decimal('0.87'), 5),
                                                    paysys_type_cc=PaysysType.MONEY if payment_type == PartnerPaymentType.WALLET else PaysysType.PAYPAL))

    utils.check_that(actual_payments, contains_dicts_with_entries(expected_payments),
                     u"Проверяем наличие ожидаемых платежей")
