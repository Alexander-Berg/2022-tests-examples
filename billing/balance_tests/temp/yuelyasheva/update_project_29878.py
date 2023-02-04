# -*- coding: utf-8 -*-
import uuid
from datetime import datetime, timedelta

import pytest
import hamcrest

import btestlib.reporter as reporter
from balance import balance_steps as steps
import balance.balance_db as db
from balance.features import Features
from btestlib import utils as utils
from btestlib.constants import Managers, ContractPaymentType, Firms, Services, Currencies, ContractSubtype

CURRENT_DT = datetime.today()
START_DT_FUTURE = CURRENT_DT + timedelta(days=5)
START_DT_PAST = CURRENT_DT + timedelta(days=-5)

MANAGER_UID = Managers.SOME_MANAGER.uid
PAYMENT_TYPE = ContractPaymentType.PREPAY
FIRM_ID = Firms.CLOUD_112.id
SERVICE_ID = Services.CLOUD_143.id
CURRENCY = Currencies.RUB.char_code


#from xmlrpc/test_CreateOffer_for_clouds
def default_data():
    client_id = steps.ClientSteps.create()
    person_id = steps.PersonSteps.create(client_id, 'ur')
    return {'client_id': client_id, 'person_id': person_id, 'firm_id': FIRM_ID, 'manager_uid': MANAGER_UID,
            'payment_type': PAYMENT_TYPE, 'services': [
            SERVICE_ID], 'currency': CURRENCY, 'projects': [str(uuid.uuid4())],
    'start_dt': START_DT_FUTURE}


#from test_UpdateProjects(BALANCE-25178)
def update_project(action, contract_id_source, contract_id_target, projects, start_dt=START_DT_FUTURE):
    map = {'add': 'add_projects', 'move': 'move_projects'}
    steps.ContractSteps.update_offer_projects({'manager_uid': MANAGER_UID, 'start_dt': start_dt, 'instructions': [
        {'action': map[action], 'source': contract_id_source, 'target': contract_id_target, 'projects': projects}]})


def create_contract():
    data = default_data()
    contract_id, contract_eid = steps.ContractSteps.create_offer(data)
    update_project('add', 'None', contract_id, data['projects'])
    attributes_data = db.balance().execute(
        "select * from t_contract_attributes \
                where collateral_id in \
                (select id from t_contract_collateral \
                where contract2_id = :contract_id and collateral_type_id = 1050) \
                and (code = 'MEMO' or code = 'PRINT_FORM_TYPE') \
                order by value_num",
                              {'contract_id': contract_id})

    memo_exp = db.get_attributes_by_attr_code(contract_id, 'MEMO', u'Ф2')
    form_exp = db.get_attributes_by_attr_code(contract_id, 'PRINT_FORM_TYPE', u'Ф2')
    utils.check_that(form_exp, hamcrest.equal_to(3))
    utils.check_that(memo_exp,
                     hamcrest.contains_string(u'Технологическое подключение к сервису Яндекс.Облако'))

def create_contract_no_projects():

    data = default_data()
    contract_id, contract_eid = steps.ContractSteps.create_offer(data)
    update_project('add', 'None', contract_id, '')

def get_contract_attributes(contract_id, attr_code, collateral_num):
    if collateral_num:
        attributes_data = db.balance().execute(
            "select * from t_contract_attributes \
                    where collateral_id in \
                    (select id from t_contract_collateral \
                    where contract2_id = :contract_id and num = :collateral_num) \
                    and (code = :attr_code) \
                    order by value_num",
            {'contract_id': contract_id, 'collateral_num': collateral_num, 'attr_code': attr_code})
    else:
        attributes_data = db.balance().execute(
            "select * from t_contract_attributes \
                    where collateral_id in \
                    (select id from t_contract_collateral \
                    where contract2_id = :contract_id) \
                    and (code = :attr_code) \
                    order by value_num",
            {'contract_id': contract_id, 'attr_code': attr_code})
    return attributes_data[0]['value_clob'] or attributes_data[0]['value_dt'] or attributes_data[0]['value_num']


def create_two_contracts_one_project():
    data = default_data()
    contract_id, contract_eid = steps.ContractSteps.create_offer(data)
    data_project = data['projects']
    update_project('add', 'None', contract_id, data_project)
    contract_id, contract_eid = steps.ContractSteps.create_offer(data)
    update_project('add', 'None', contract_id, data_project)


def create_two_contracts_not_active_one_project():
    data = default_data()
    data.update({'start_dt': START_DT_PAST + timedelta(days=-5),
                 'is_deactivated': 1})
    contract_id, contract_eid = steps.ContractSteps.create_offer(data)

    data_project = data['projects']
    update_project('add', 'None', contract_id, data_project)

    data.update({'start_dt': CURRENT_DT,
                 'end_dt': START_DT_FUTURE})
    contract_id, contract_eid = steps.ContractSteps.create_offer(data)
    update_project('add', 'None', contract_id, data_project)


#create_contract()
create_two_contracts_not_active_one_project()



#UpdateProjects не принимает пустое поле projects
#create_contract_no_projects()

#тип допника, комментарий