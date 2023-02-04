# -*- coding: utf-8 -*-

__author__ = 'quark'

import pytest

import btestlib.constants
import balance.balance_api
import balance.tests.xmlrpc.test_GetContractPrintForm
from balance.balance_steps import ContractSteps

ADMIN_PASSPORT_ID = btestlib.constants.Managers.PRINT_FORM_MANAGER.uid
cases = [balance.tests.xmlrpc.test_GetContractPrintForm.ContractPrintFormCases.TAXI_CORP_POST_1]


@pytest.mark.parametrize('print_form_case', cases, ids=lambda pfc: pfc.name)
def test_send_email(print_form_case):
    # Создаем неподписанный договор
    contract_ids = print_form_case.create_contract()
    if isinstance(contract_ids, int):
        contract_ids = [contract_ids, ]

    for contract_id in contract_ids:

        params = {
            'object_type': 'contract',
            'object_id': contract_id,
            'extra_data': {
                'email_to': btestlib.constants.Emails.DUMMY_EMAIL,
                'email_from': btestlib.constants.Emails.DUMMY_EMAIL,
                'email_to_client': '',
                'email_to_manager': '',
                'email_subject': 'Balance test',
                'email_body': ''
            }
        }
        create_message(params)
        sign_contract(params)
        send_email(params)


def create_message(params):
    balance.balance_api.test_balance().EnqueuePrintFormEmail(ADMIN_PASSPORT_ID, params)


def get_collateral_id(params):
    if params['object_type'] == 'contract':
        res = balance.balance_api.test_balance().ExecuteSQL(
            'balance',
            "select * from bo.t_contract_collateral where contract2_id = :object_id and num is null",
            params)
        collateral_id = res[0]['id']
    else:
        collateral_id = params['object_id']
    return collateral_id


def sign_contract(params):
    if params['object_type'] != 'contract':
        # currently, the only usage of this function (balance.tests.email_message.test_print_forms.test_send_email)
        # passes contract_id
        raise RuntimeError('Expected to find contract data')
    contract_id = params['object_id']
    collateral_id = get_collateral_id(params)
    res = balance.balance_api.test_balance().ExecuteSQL(
        'balance',
        'update bo.t_contract_collateral set is_signed = sysdate - 1 where id = :collateral_id',
        {'collateral_id': collateral_id})
    ContractSteps.refresh_contracts_cache(contract_id)
    return res


def send_email(params):
    collateral_id = get_collateral_id(params)

    res = balance.balance_api.test_balance().ExecuteSQL(
        'balance',
        'select id from (select id from bo.t_message where object_id = :collateral_id order by dt desc) where rownum = 1',
        {'collateral_id': collateral_id})

    message_id = res[0]['id']

    return balance.balance_api.test_balance().ExportObject('EMAIL_MESSAGE', 'EmailMessage', message_id)
