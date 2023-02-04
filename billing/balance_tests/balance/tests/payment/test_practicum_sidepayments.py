# -*- coding: utf-8 -*-
import datetime

import pytest

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import TransactionType, Export, PaymentType
from btestlib.data import simpleapi_defaults
from btestlib.data.partner_contexts import PRACTICUM_RU_CONTEXT_SPENDABLE
from btestlib.matchers import contains_dicts_with_entries

contract_start_dt = datetime.datetime.today().replace(day=1)

PAYMENT_TYPES = [
    PaymentType.PRACTICUM_FLOW_1,
    PaymentType.PRACTICUM_FLOW_2,
]


def create_clients_persons_contracts_sidepayments(context):
    client_id = steps.ClientSteps.create()
    _, person_id, contract_id, _ = steps.ContractSteps. \
        create_partner_contract(context, client_id=client_id, additional_params={'start_dt': contract_start_dt,
                                                                                 'selfemployed': 1},
                                partner_integration_params={
                                    'link_integration_to_client': 1,
                                    'link_integration_to_client_args': {
                                        'integration_cc': 'practicum_partner',
                                        'configuration_cc': 'practicum_partner_default_conf',
                                    },
                                    'set_integration_to_contract': 1,
                                    'set_integration_to_contract_params': {
                                        'integration_cc': 'practicum_partner',
                                    },
                                })
    return client_id, person_id, contract_id


@pytest.mark.smoke
@pytest.mark.parametrize('context', [
    PRACTICUM_RU_CONTEXT_SPENDABLE,
])
def test_practicum_sidepayment(context):
    client_id, person_id, contract_id = create_clients_persons_contracts_sidepayments(context=context)
    for payment_type in PAYMENT_TYPES:
        # создаем сайдпеймент
        side_payment_id, transaction_id_payment = \
            steps.PartnerSteps.create_sidepayment_transaction(client_id, utils.Date.moscow_offset_dt(),
                                                              simpleapi_defaults.DEFAULT_PRICE,
                                                              payment_type,
                                                              context.service.id,
                                                              currency=context.currency,
                                                              extra_dt_0=datetime.datetime.now())

        # запускаем обработку сайдпеймента:
        steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                          Export.Classname.SIDE_PAYMENT, service_id=context.service.id)

        expected_payment = steps.SimpleApi.create_expected_tpt_row(context, client_id,
                                                                   contract_id,
                                                                   person_id, transaction_id_payment,
                                                                   side_payment_id,
                                                                   payment_type=payment_type,
                                                                   internal=1)
        payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(side_payment_id,
                                                                                         source='sidepayment')
        utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                         'Сравниваем платеж с шаблоном')


@pytest.mark.parametrize('context', [
    PRACTICUM_RU_CONTEXT_SPENDABLE,
])
def test_practicum_sidepayment_refund(context):
    client_id, person_id, contract_id = create_clients_persons_contracts_sidepayments(context)

    for payment_type in PAYMENT_TYPES:
        # создаем сайд пеймент
        side_payment_id, transaction_id_payment = \
            steps.PartnerSteps.create_sidepayment_transaction(client_id, utils.Date.moscow_offset_dt(),
                                                              simpleapi_defaults.DEFAULT_PRICE,
                                                              payment_type,
                                                              context.service.id,
                                                              currency=context.currency)

        # создаем рефанд в сайд пеймент
        side_refund_id, transaction_id_refund = \
            steps.PartnerSteps.create_sidepayment_transaction(client_id, utils.Date.moscow_offset_dt(),
                                                              simpleapi_defaults.DEFAULT_PRICE,
                                                              payment_type,
                                                              context.service.id,
                                                              currency=context.currency,
                                                              transaction_type=TransactionType.REFUND,
                                                              orig_transaction_id=transaction_id_payment)

        # запускаем обработку платежа сайдпеймента:
        steps.ExportSteps.create_export_record_and_export(side_payment_id, Export.Type.THIRDPARTY_TRANS,
                                                          Export.Classname.SIDE_PAYMENT, service_id=context.service.id)

        # запускаем обработку рефанда сайдпеймента (объект в экспорте создал платеж простановкой связанных рефандов):
        steps.ExportSteps.create_export_record_and_export(side_refund_id, Export.Type.THIRDPARTY_TRANS,
                                                          Export.Classname.SIDE_PAYMENT, service_id=context.service.id,
                                                          with_export_record=False)

        expected_payment = steps.SimpleApi.create_expected_tpt_row(context, client_id,
                                                                   contract_id,
                                                                   person_id, transaction_id_payment,
                                                                   side_refund_id, transaction_id_refund,
                                                                   payment_type=payment_type,
                                                                   internal=1)
        payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(side_refund_id,
                                                                                         transaction_type=TransactionType.REFUND,
                                                                                         source='sidepayment')
        utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                         'Сравниваем платеж с шаблоном')
