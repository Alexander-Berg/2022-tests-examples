# coding: utf-8
from apikeys import apikeys_steps_new
from apikeys.apikeys_api import UI2, BO, BO2, Questionary,API

#Для ТК данной группы зарезервированы пользователи от 1 до 50

def test_Link_TK_1_2_10(db_connection):
    #TK 1, 2, 10
    #yndx-api-assessor-1..... yndx-api-assessor-10
    #В данном скирипте пользователи очищаются полностью в томи числе удалется и запись из бызы пользвателей Апикейс

    Case_logins = ['yndx-api-assessor-{}'.format(num) for num in range(1, 9)]
    logins_id = db_connection['user'].find({'login': {'$in': Case_logins}}, {'_id': 1})
    for item in logins_id:
        apikeys_steps_new.clean_up(item['_id'], db_connection)
        db_connection['user'].delete_one({'_id': item['_id']})



def test_Link_TK_11(db_connection):
    #yndx-api-assessor-11 891171400
    #yndx-api-assessor-12 891171604
    #yndx-api-assessor-13 891171854
    #yndx-api-assessor-14 891172029
    #yndx-api-assessor-15 891172286


    uids = [891171400, 891171604, 891171854, 891172029, 891172286]
    for uid in uids:
        apikeys_steps_new.clean_up(uid, db_connection)
        services = db_connection['service'].find({'attachable_in_ui': True, 'questionnaire_id':{'$exists': True}},{'cc':True, 'questionnaire_id': True})

        project_id = UI2().project_create(oper_uid=uid).json()['data']['id']

        for service in services:
            if service.get('questionnaire_id', False):
                Questionary.attach_project(service_cc=service['cc'], project_id=project_id)
            else:
                UI2().project_service_link_create(uid, project_id, int(service['_id']))



def test_Link_Project_TK_15(db_connection):
    # yndx-api-assessor-16 891172504
    # yndx-api-assessor-17 891172726
    # yndx-api-assessor-18 891172921
    # yndx-api-assessor-19 891173135
    uids = [891172504, 891172726, 891172921, 891173135]
    for uid in uids:
        apikeys_steps_new.clean_up(uid, db_connection)
        project_id = UI2().project_create(oper_uid=uid).json()['data']['id']
        Questionary.attach_project(service_cc='apimaps', project_id=project_id)
        Questionary.attach_project(service_cc='rasp', project_id=project_id)