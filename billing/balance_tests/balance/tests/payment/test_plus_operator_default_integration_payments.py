# coding: utf-8
__author__ = 'iuriiz'

import pytest

import btestlib.utils as utils
from balance import balance_steps as steps
from balance.features import Features
from btestlib import constants as cst
from btestlib.data.partner_contexts import INVESTMENTS_CONTEXT, USLUGI_CONTEXT
from btestlib.matchers import contains_dicts_with_entries
import btestlib.reporter as reporter


pytestmark = [
    reporter.feature(Features.TRUST, Features.PAYMENT),
    pytest.mark.usefixtures("switch_to_pg")
]

parametrize_context = pytest.mark.parametrize('context', [
    INVESTMENTS_CONTEXT, USLUGI_CONTEXT
], ids=lambda x: x.name)

rbs_params = {
    'broker_data': {
        'client_code': '!@#$%^&*',
        'account_number': '9' * 20
    }
}


@pytest.fixture(autouse=True)
def mock_trust(mock_simple_api):
    pass


@parametrize_context
@pytest.mark.parametrize(
    'partner_integration_params',
    [
        pytest.param(steps.CommonIntegrationSteps.DEFAULT_PARTNER_INTEGRATION_PARAMS_FOR_CREATE_CONTRACT,
                     id='PARTNER_INTEGRATION'),
        pytest.param(None, id='WO_PARTNER_INTEGRATION'),
    ]
)
def test_payment(context, partner_integration_params):
    # simple partner instead of force_partner because multiple service configurations are not allowed for single client
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, partner_integration_params=partner_integration_params
    )

    service_product_id = steps.SimpleApi.create_service_product(context.service)
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(context.service, service_product_id,
                                             commission_category=None,
                                             **get_additional_service_payment_params(context)
                                             )
    tech_client_id, tech_person_id, tech_contract_id = steps.CommonPartnerSteps.get_active_tech_ids(context.service)
    expected_payment = steps.SimpleApi.create_expected_tpt_row(
        context, tech_client_id, tech_contract_id, tech_person_id,
        trust_payment_id, payment_id,
        internal=1,
    )

    export_and_check_payment(payment_id, [expected_payment])


@parametrize_context
@pytest.mark.parametrize(
    'partner_integration_params',
    [
        pytest.param(steps.CommonIntegrationSteps.DEFAULT_PARTNER_INTEGRATION_PARAMS_FOR_CREATE_CONTRACT,
                     id='PARTNER_INTEGRATION'),
        pytest.param(None, id='WO_PARTNER_INTEGRATION'),
    ]
)
def test_refund(context, partner_integration_params):
    # simple partner instead of force_partner because multiple configurations are not allowed for single client
    client_id, person_id, contract_id, _ = steps.ContractSteps.create_partner_contract(
        context, partner_integration_params=partner_integration_params
    )

    service_product_id = steps.SimpleApi.create_service_product(context.service)
    service_order_id, trust_payment_id, purchase_token, payment_id = \
        steps.SimpleApi.create_trust_payment(context.service, service_product_id,
                                             commission_category=None,
                                             **get_additional_service_payment_params(context)
                                             )
    trust_refund_id, refund_id = steps.SimpleApi.create_refund(context.service,
                                                               service_order_id, trust_payment_id)
    tech_client_id, tech_person_id, tech_contract_id = steps.CommonPartnerSteps.get_active_tech_ids(context.service)
    expected_payment = steps.SimpleApi.create_expected_tpt_row(
        context, tech_client_id, tech_contract_id, tech_person_id,
        trust_payment_id, payment_id,
        internal=1,
    )

    expected_refund = steps.SimpleApi.create_expected_tpt_row(
        context, tech_client_id, tech_contract_id, tech_person_id,
        trust_payment_id, payment_id,
        trust_refund_id=trust_refund_id,
        internal=1,
    )
    export_and_check_payment(payment_id, [expected_payment])
    export_and_check_payment(refund_id, [expected_refund], payment_id, cst.TransactionType.REFUND)


def get_additional_service_payment_params(context):
    if context.service.id == INVESTMENTS_CONTEXT.service.id:
        return {'pass_cvn': True, 'pass_params': rbs_params}
    return {}


def export_and_check_payment(payment_id, expected_data, thirdparty_payment_id=None,
                             transaction_type=cst.TransactionType.PAYMENT):
    if not thirdparty_payment_id:
        thirdparty_payment_id = payment_id

    steps.CommonPartnerSteps.export_payment(payment_id)

    payment_data = steps.CommonPartnerSteps.get_thirdparty_transaction_by_payment_id(
        thirdparty_payment_id, transaction_type)
    utils.check_that(payment_data, contains_dicts_with_entries(expected_data), u'Сравниваем платеж с шаблоном')
