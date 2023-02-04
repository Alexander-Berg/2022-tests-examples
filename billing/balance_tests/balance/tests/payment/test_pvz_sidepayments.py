# -*- coding: utf-8 -*-

__author__ = 'sfreest'

import datetime
import pytest

from balance import balance_steps as steps
from btestlib import utils
from btestlib.constants import TransactionType, Export, PaymentType, Services
from btestlib.data import simpleapi_defaults
from btestlib.data.partner_contexts import PVZ_RU_CONTEXT_SPENDABLE, SORT_CENTER_CONTEXT_SPENDABLE, \
    DELIVERY_SERVICES_CONTEXT_SPENDABLE
from btestlib.matchers import contains_dicts_with_entries

contract_start_dt = datetime.datetime.today().replace(day=1)

PAYMENT_TYPES_BY_SERVICE = {
    Services.PVZ.id: (
        PaymentType.PVZ_REWARD,
        PaymentType.PVZ_DROPOFF,
        PaymentType.PVZ_BRANDED_DECORATION,
    ),
    Services.SORT_CENTER.id: (
        PaymentType.ACC_SORTING_REWARD,
        PaymentType.ACC_SORTING_RETURN_REWARD,
        PaymentType.ACC_STORING_REWARD,
        PaymentType.ACC_STORING_RETURN_REWARD,
    ),
    Services.MARKET_DELIVERY_SERVICES.id: (
        PaymentType.ACC_CAR_DELIVERY,
        PaymentType.ACC_TRUCK_DELIVERY,
        PaymentType.ACC_LOAD_UNLOAD,
    ),
}

INTEGRATION_MAP = {
    Services.PVZ.id: {
        'integration_cc': 'market_logistics_partner',
        'configuration_cc': 'market_logistics_partner_default_conf',
    },
    Services.SORT_CENTER.id: {
        'integration_cc': 'market_sort_centers',
        'configuration_cc': 'market_sort_centers_default',
    },
    Services.MARKET_DELIVERY_SERVICES.id: {
        'integration_cc': 'market_delivery_services',
        'configuration_cc': 'market_delivery_services_default',
    },
}

SERVICES_PARAMETRIZE = pytest.mark.parametrize(
    'context, internal',
    (
        pytest.param(PVZ_RU_CONTEXT_SPENDABLE, 1,
                     id='PVZ_RU_CONTEXT_SPENDABLE'),
        # pytest.param(SORT_CENTER_CONTEXT_SPENDABLE, None,
        #              id='SORT_CENTER_CONTEXT_SPENDABLE'),
        # pytest.param(DELIVERY_SERVICES_CONTEXT_SPENDABLE, None,
        #              id='DELIVERY_SERVICES_CONTEXT_SPENDABLE'),
    ))


def create_clients_persons_contracts_sidepayments(context):
    client_id = steps.ClientSteps.create()
    _, person_id, contract_id, _ = steps.ContractSteps. \
        create_partner_contract(context, client_id=client_id,
                                additional_params={'start_dt': contract_start_dt},
                                partner_integration_params={
                                    'link_integration_to_client': 1,
                                    'link_integration_to_client_args': INTEGRATION_MAP[context.service.id],
                                    'set_integration_to_contract': 1,
                                    'set_integration_to_contract_params': INTEGRATION_MAP[context.service.id],
                                })
    return client_id, person_id, contract_id


@pytest.mark.smoke
@SERVICES_PARAMETRIZE
def test_pvz_sidepayment(context, internal):
    client_id, person_id, contract_id = create_clients_persons_contracts_sidepayments(context=context)
    payment_types = PAYMENT_TYPES_BY_SERVICE[context.service.id]
    for payment_type in payment_types:
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
                                                                   internal=internal)
        payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(side_payment_id,
                                                                                         source='sidepayment')
        utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                         'Сравниваем платеж с шаблоном')


@SERVICES_PARAMETRIZE
def test_pvz_sidepayment_refund(context, internal):
    client_id, person_id, contract_id = create_clients_persons_contracts_sidepayments(context)

    payment_types = PAYMENT_TYPES_BY_SERVICE[context.service.id]
    for payment_type in payment_types:
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
                                                              orig_transaction_id=transaction_id_payment, )

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
                                                                   internal=internal)
        payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(side_refund_id,
                                                                                         transaction_type=TransactionType.REFUND,
                                                                                         source='sidepayment')
        utils.check_that(payment_data, contains_dicts_with_entries([expected_payment]),
                         'Сравниваем платеж с шаблоном')
