# -*- coding: utf-8 -*-

from btestlib.constants import ContractPaymentType


def prepay_contract(
        contract_type,
        person_type,
        firm_id,
        service_list,
        currency,
        additional_params=None,
):
    if additional_params is None:
        additional_params = {}

    default_params = {
        'contract_type': contract_type,
        'person': person_type,
        'contract_params': {
            'SERVICES': service_list,
            'FIRM': firm_id,
            'CURRENCY': currency,
            'PAYMENT_TYPE': ContractPaymentType.PREPAY,
        },
    }
    if additional_params:
        default_params['contract_params'].update(additional_params)
    return default_params


def post_pay_contract_personal_account_fictive(
        contract_type,
        person_type,
        firm_id,
        service_list,
        currency,
        additional_params=None,
):
    if additional_params is None:
        additional_params = {}

    default_params = {
        'contract_type': contract_type,
        'person': person_type,
        'contract_params': {
            'SERVICES': service_list,
            'FIRM': firm_id,
            'CURRENCY': currency,
            'PAYMENT_TYPE': ContractPaymentType.POSTPAY,
            'REPAYMENT_ON_CONSUME': 1,
            'PERSONAL_ACCOUNT': 1,
            'PERSONAL_ACCOUNT_FICTIVE': 1,
        },
    }
    if additional_params:
        default_params['contract_params'].update(additional_params)
    return default_params
