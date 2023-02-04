# coding: utf-8
from apikeys import apikeys_steps_new
from apikeys.apikeys_api import UI2, BO, BO2, Questionary, API


# Для данных ТК зарезервированный пользователи от 51 до 100

def test_tk_3_17(db_connection):
    # ТК 3
    # yndx-api-assessor-51 891181459
    # yndx-api-assessor-52 891181760
    # yndx-api-assessor-53 891181963
    # yndx-api-assessor-54 891182224

    uids = [891181459, 891181760, 891181963, 891182224]
    for uid in uids:
        apikeys_steps_new.clean_up(uid, db_connection)
        project_id = UI2().project_create(oper_uid=uid).json()['data']['id']
        Questionary.attach_project(service_cc='apimaps', project_id=project_id)



def test_tk_5_6_7_17(db_connection):
    # yndx-api-assessor-55 891182459
    # yndx-api-assessor-56 891182748
    # yndx-api-assessor-57 910509562
    # yndx-api-assessor-58 910509602
    # yndx-api-assessor-59 910509619

    uids = [891182459, 891182748, 910509562, 910509602, 910509619]
    for uid in uids:
        apikeys_steps_new.clean_up(uid, db_connection)
        project_id = UI2().project_create(oper_uid=uid).json()['data']['id']
        Questionary.attach_project(service_cc='apimaps', project_id=project_id)
        Questionary.attach_project(service_cc='mapkit', project_id=project_id)
        try:
            UI2().key_create(oper_uid=uid, project_id=project_id, service_id='3', data={'name': 'I_alone_key'})
            UI2().key_create(oper_uid=uid, project_id=project_id, service_id='3', data={'name': 'I_am_one_of_two'})
            UI2().key_create(oper_uid=uid, project_id=project_id, service_id='3', data={'name': 'I_am_one_of_two'})
            UI2().key_create(oper_uid=uid, project_id=project_id, service_id='3', data={'name': 'Я_ключ_на_кириллице'})

            block_key = UI2().key_create(oper_uid=uid, project_id=project_id, service_id='3',
                                       data={'name': 'I_am_blocked_key'}).json()['data']['id']
            UI2().key_update(oper_uid=uid, project_id=project_id, service_id='3', key= block_key, data = {"active":False})
            for num in range(10):
                UI2().key_create(oper_uid=uid, project_id=project_id, service_id='3',
                               data={'name': '_key №{}'.format(num)})
            UI2().key_create(oper_uid=uid, project_id=project_id, service_id='33', data={'name': 'Key 1'})
        except:
            continue
