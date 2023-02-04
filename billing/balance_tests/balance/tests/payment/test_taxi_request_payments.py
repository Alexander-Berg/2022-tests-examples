# -*- coding: utf-8 -*-

__author__ = 'mindlin'

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import TransactionType, Services, Export, PaymentType
from btestlib.data import simpleapi_defaults
from btestlib.data.partner_contexts import TAXI_REQUEST_CONTEXT_SPENDABLE
from btestlib.matchers import contains_dicts_with_entries
from simpleapi.common.payment_methods import Cash

PAYMETHOD = Cash()

contract_start_dt = datetime.datetime.today().replace(day=1)


def create_clients_persons_contracts_sidepayments():
    client_id, person_id, contract_id, _ = steps.ContractSteps. \
        create_partner_contract(TAXI_REQUEST_CONTEXT_SPENDABLE,
                                additional_params={'start_dt': contract_start_dt})

    return client_id, person_id, contract_id


@reporter.feature(Features.TAXI_REQUEST)
@pytest.mark.smoke
def test_taxi_request_sidepayment():
    client_id, person_id, contract_id = create_clients_persons_contracts_sidepayments()

    # создаем сайдпеймент
    side_payment_id, transaction_id_payment = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, utils.Date.moscow_offset_dt(),
                                                          simpleapi_defaults.DEFAULT_PRICE,
                                                          PaymentType.CASH,
                                                          Services.TAXI_REQUEST.id,
                                                          currency=TAXI_REQUEST_CONTEXT_SPENDABLE.currency,
                                                          extra_dt_0=datetime.datetime.now())

    # запускаем обработку сайдпеймента:
    steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    expected_payment = steps.SimpleApi.create_expected_tpt_row(TAXI_REQUEST_CONTEXT_SPENDABLE, client_id,
                                                               contract_id,
                                                               person_id, transaction_id_payment,
                                                               side_payment_id,
                                                               payment_type=PaymentType.CASH)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(side_payment_id,
                                                                                     source='sidepayment')
    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                     'Сравниваем платеж с шаблоном')


@reporter.feature(Features.TAXI_REQUEST)
def test_taxi_request_sidepayment_refund():
    client_id, person_id, contract_id = create_clients_persons_contracts_sidepayments()

    # создаем сайд пеймент
    side_payment_id, transaction_id_payment = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, utils.Date.moscow_offset_dt(),
                                                          simpleapi_defaults.DEFAULT_PRICE,
                                                          PaymentType.CASH,
                                                          Services.TAXI_REQUEST.id,
                                                          currency=TAXI_REQUEST_CONTEXT_SPENDABLE.currency,
                                                          extra_dt_0=datetime.datetime.now())

    # создаем рефанд в сайд пеймент
    side_refund_id, transaction_id_refund = \
        steps.PartnerSteps.create_sidepayment_transaction(client_id, utils.Date.moscow_offset_dt(),
                                                          simpleapi_defaults.DEFAULT_PRICE,
                                                          PaymentType.CASH,
                                                          Services.TAXI_REQUEST.id,
                                                          currency=TAXI_REQUEST_CONTEXT_SPENDABLE.currency,
                                                          extra_dt_0=datetime.datetime.now(),
                                                          transaction_type=TransactionType.REFUND,
                                                          orig_transaction_id=transaction_id_payment)

    # запускаем обработку рефанда сайдпеймента:
    steps.ExportSteps.create_export_record_and_export(side_refund_id, Export.Type.THIRDPARTY_TRANS,
                                                      Export.Classname.SIDE_PAYMENT)

    expected_payment = steps.SimpleApi.create_expected_tpt_row(TAXI_REQUEST_CONTEXT_SPENDABLE, client_id,
                                                               contract_id,
                                                               person_id, transaction_id_payment,
                                                               side_refund_id, transaction_id_refund,
                                                               payment_type=PaymentType.CASH)
    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(side_refund_id,
                                                                                     transaction_type=TransactionType.REFUND,
                                                                                     source='sidepayment')
    utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                     'Сравниваем платеж с шаблоном')
