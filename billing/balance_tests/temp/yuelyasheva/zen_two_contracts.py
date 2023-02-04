# -*- coding: utf-8 -*-

__author__ = 'yuelyasheva'

import datetime
from decimal import Decimal as D

import pytest

import btestlib.reporter as reporter
from balance import balance_steps as steps
from balance.features import Features
from btestlib import utils
from btestlib.constants import TransactionType
from btestlib.data import defaults
from btestlib.data.defaults import SpendableContractDefaults as SpendableDefParams
from btestlib.data.simpleapi_defaults import ThirdPartyData
from btestlib.matchers import contains_dicts_equal_to
from btestlib.constants import Users, Nds, Currencies, Managers, Services, Firms, Regions, PersonTypes, Paysyses
from dateutil.relativedelta import relativedelta

START_DT = utils.Date.first_day_of_month() - relativedelta(months=2)
AMOUNT = D('1000.1')

def create_offer(is_resident=False):
    client_id = steps.ClientSteps.create()

    linked_person_id = None
    linked_contract_id = None
    if is_resident:
        linked_person_id = steps.PersonSteps.create(client_id, PersonTypes.PH.code, params={'is-partner': '1'})
        linked_contract_id = create_offer_only(client_id, linked_person_id, Firms.ZEN_28)

    person_id = steps.PersonSteps.create(client_id, PersonTypes.SW_YTPH.code, params={'is-partner': '1'})
    contract_id = create_offer_only(client_id, person_id, linked_contract_id=linked_contract_id)

    return client_id, linked_person_id if is_resident else person_id, linked_contract_id if is_resident else contract_id

def create_offer_only(client_id, person_id, firm=Firms.SERVICES_AG_16, linked_contract_id=None):
    contract_id, _ = steps.ContractSteps.create_offer(utils.remove_empty({
        'client_id': client_id,
        'person_id': person_id,
        'manager_uid': Managers.SOME_MANAGER.uid,
        'personal_account': 1,
        'currency': Currencies.RUB.char_code,
        'firm_id': firm.id,
        'services': [Services.ZEN.id],
        'payment_term': 10,
        'start_dt': START_DT,
        'nds': Nds.DEFAULT,
        'link_contract_id': linked_contract_id
    }))

    return contract_id

#lient_id, person_id , _ = create_offer()


client_id = 104956730
person_id = 8870726

create_offer_only(client_id, person_id)