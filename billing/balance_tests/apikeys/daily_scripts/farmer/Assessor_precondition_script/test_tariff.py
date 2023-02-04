# coding: utf-8
from apikeys import apikeys_steps_new
from apikeys.apikeys_api import UI2, BO, BO2, Questionary, API
from balance import balance_api as api, balance_steps as steps, balance_db
from apikeys.apikeys_defaults import ADMIN

def test_tk_8_9(db_connection):
    # ТК 3
    # yndx-api-assessor-101 891171210
    # yndx-api-assessor-102 891171229
    # yndx-api-assessor-103 891171243
    # yndx-api-assessor-104 891171258
    uids = [891171210, 891171229, 891171243, 891171258]
    for uid in uids:
        apikeys_steps_new.clean_up(uid, db_connection)
        project_id = UI2().project_create(oper_uid=uid).json()['data']['id']
        Questionary.attach_project(service_cc='apimaps', project_id=project_id)
        Questionary.attach_project(service_cc='rasp', project_id=project_id)


def test_tk_16(db_connection):

    uids = {'yndx-api-assessor-105': 891171280,
            'yndx-api-assessor-106': 891171301,
            'yndx-api-assessor-107': 891171320,
            'yndx-api-assessor-108': 891171352,
            'yndx-api-assessor-109': 891171382
            }

    for login, uid in uids.iteritems():
        apikeys_steps_new.clean_up(uid, db_connection)
        project_id = UI2().project_create(oper_uid=uid).json()['data']['id']
        Questionary.attach_project(service_cc='apimaps', project_id=project_id)
        client_id = steps.ClientSteps.create()
        steps.ClientSteps.link(client_id, login)
        person_id = steps.PersonSteps.create(client_id, type_='ur')
        person_id2 = steps.PersonSteps.create(client_id, type_='ur')
        BO.get_client_from_balance(ADMIN, uid)
        UI2().project_service_link_update(uid, project_id, 3,
                                                    {'scheduled_tariff_cc': 'apimaps_50000_yearprepay_contractless_032019'})