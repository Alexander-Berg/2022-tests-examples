# -*- coding: utf-8 -*-
__author__ = 'atkaya'

import datetime

import pytest

import btestlib.reporter as reporter
from balance import balance_api as api
from balance import balance_steps as steps
from balance.features import Features

pytestmark = [
    reporter.feature(Features.PARTNER, Features.GET_PARTNER_CONTRACTS, Features.XMLRPC)
]


def create_client_contract(is_collateral_needed=0):
    # создаем партнера и плательщика
    client_id, person_id = steps.PartnerSteps.create_partner_client_person()

    # создаем универсальный РСЯ договор
    contract_id, _ = steps.ContractSteps.create_contract('rsya_universal',
                                                         {'CLIENT_ID': client_id, 'PERSON_ID': person_id})

    if is_collateral_needed:
        dt = datetime.datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
        steps.ContractSteps.create_collateral(2010, {'CONTRACT2_ID': contract_id,
                                                     'DT': dt,
                                                     'IS_SIGNED': dt.isoformat()})
    return client_id


@reporter.feature(Features.TO_UNIT)
@pytest.mark.smoke
@pytest.mark.tickets('BALANCE-24743')
def test_get_partner_contracts_wo_collaterals():
    # создаем договор и клиента
    client_id = create_client_contract()

    # вызываем GetPartnerContracts и записываем ответ
    api.medium().GetPartnerContracts({'ClientID': client_id})


@reporter.feature(Features.TO_UNIT)
@pytest.mark.tickets('BALANCE-24745')
def test_get_partner_contracts_with_collaterals():
    # создаем договор и клиента
    client_id = create_client_contract(is_collateral_needed=1)

    # вызываем GetPartnerContracts и записываем ответ
    api.medium().GetPartnerContracts({'ClientID': client_id})


@reporter.feature(Features.TO_UNIT)
@pytest.mark.tickets('BALANCE-24743')
def test_get_partner_contracts_wo_contract():
    # создаем клиента
    client_id = steps.ClientSteps.create()

    # вызываем GetPartnerContracts и записываем ответ
    api.medium().GetPartnerContracts({'ClientID': client_id})
