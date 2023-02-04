# -*- coding: utf-8 -*-
__author__ = 'yuelyasheva'

import uuid
from datetime import datetime, timedelta
from xmlrpclib import Fault

import pytest
from hamcrest import equal_to, contains_string

import balance.balance_db as db
from balance import balance_steps as steps
from btestlib import utils as utils
from btestlib.constants import Managers, ContractPaymentType, Firms, Services, Currencies, PersonTypes
from balance.features import Features
import btestlib.reporter as reporter

CURRENT_DT = datetime.today()
START_DT_FUTURE = CURRENT_DT + timedelta(days=5)
START_DT_PAST = CURRENT_DT + timedelta(days=-5)

MANAGER_UID = Managers.SOME_MANAGER.uid
PAYMENT_TYPE = ContractPaymentType.PREPAY
FIRM_ID = Firms.CLOUD_112.id
SERVICE_ID = Services.CLOUD_143.id
CURRENCY = Currencies.RUB.char_code


def update_project(action, contract_id_source, contract_id_target, projects, start_dt=START_DT_FUTURE):
    map = {'add': 'add_projects', 'move': 'move_projects'}
    steps.ContractSteps.update_offer_projects({'manager_uid': MANAGER_UID, 'start_dt': start_dt, 'instructions': [
        {'action': map[action], 'source': contract_id_source, 'target': contract_id_target, 'projects': projects}]})


#BALANCE-29878 - проверка создания допника через UpdateProjects
def test_update_project_cloud():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    data = {'client_id': client_id, 'person_id': person_id, 'firm_id': FIRM_ID,
            'manager_uid': MANAGER_UID, 'payment_type': PAYMENT_TYPE,
            'services': [SERVICE_ID], 'currency': CURRENCY, 'projects': [str(uuid.uuid4())],
            'start_dt': START_DT_FUTURE}

    contract_id, contract_eid = steps.ContractSteps.create_offer(data)
    update_project('add', 'None', contract_id, data['projects'])

    memo_exp = db.get_attributes_by_attr_code(contract_id, 'MEMO', 'Ф2')
    form_exp = db.get_attributes_by_attr_code(contract_id, 'PRINT_FORM_TYPE', 'Ф2')
    utils.check_that(form_exp, equal_to(3))
    utils.check_that(memo_exp,
                     contains_string(u'Технологическое подключение к сервису Яндекс.Облако'))


@reporter.feature(Features.TO_UNIT)
def test_update_project_old_date():
    start_dt = utils.Date.get_last_day_of_previous_month()

    contract_id, projects = create_contract()

    with pytest.raises(Fault) as error:
        update_project('add', 'None', contract_id, projects, start_dt)

    utils.check_that(error.value.faultString,
                     contains_string('Illegal argument: start_dt can not be earlier than first day of currency month'),
                     u'Проверяем текст ошибки')


# --------------------------------------------------------------
# Utils

def create_contract():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, PersonTypes.UR.code)
    data = {
        'client_id': client_id,
        'person_id': person_id,
        'firm_id': FIRM_ID,
        'manager_uid': MANAGER_UID,
        'payment_type': PAYMENT_TYPE,
        'services': [SERVICE_ID],
        'currency': CURRENCY,
        'projects': [str(uuid.uuid4())],
        'start_dt': START_DT_FUTURE
    }

    contract_id, contract_eid = steps.ContractSteps.create_offer(data)
    return contract_id, data['projects']
