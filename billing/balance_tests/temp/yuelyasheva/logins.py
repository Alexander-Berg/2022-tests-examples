# coding: utf-8

import balance.balance_steps as steps

import json
import datetime

import balance.balance_api as api
import balance.balance_db as db
import btestlib.environments as env
import btestlib.passport_steps as passport_steps
import btestlib.reporter as reporter
import btestlib.shared as shared
import btestlib.utils as utils
from btestlib import utils_tvm
from btestlib.data import defaults
from simpleapi.common.utils import call_http


def login_generator():
    login_prefix = ['yb-tst-role-60']
    postfixs = ['-rn-nc', '-rn-c', '-rf-c', '-rc-c', '-rfc-c']
    logins = []
    for prefix in login_prefix:
        for postfix in postfixs:
            logins.append(prefix+postfix)
    print logins


# login_generator()

def get_passport():
    logins = ['yb-tst-role-10-rn-nc', 'yb-tst-role-10-rn-c', 'yb-tst-role-10-rf-c', 'yb-tst-role-10-rc-c', 'yb-tst-role-10-rfc-c']
    for login in logins:
        data = steps.api.medium().GetPassportByLogin(0, login)

# get_passport()

def add_permissions_to_role(role_id, perm_ids):
    with reporter.step(u'Добавляем права в роль'):
        query = 'INSERT INTO t_role (perm, role_id) VALUES (:perm_id, :role_id)'
        for perm_id in perm_ids:
            db.balance().execute(query, {'perm_id': perm_id, 'role_id': role_id})

def create_client_restr_roles():
    perm_ids = [# '1':
                    [0, 1, 2, 4, 7, 25, 31, 26, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 19, 20, 22, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009, 1010, 1011, 1012, 1013, 1100, 23, 27, 28, 30, 24, 31, 32, 33, 34, 37, 38, 39, 41, 3001, 43, 46, 47, 57, 56, 58, 60, 62, 63, 51, 65, 11101, 11104, 11112, 11106, 11105, 11108, 11107, 11111, 11110, 11121, 11141, 11142, 11143, 11181, 11103, 11102, 11201, 11221, 11222, 11226, 11227, 11224, 11223, 11225, 66, 67, 11281, 11321, 11322, 11323, 11324, 11325, 11326, 64],
        # [0, 1, 2, 4, 7, 25, 31, 26, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 19, 20, 22, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009, 1010, 1011, 1012, 1013, 23, 27, 28, 30, 24, 31, 32, 33, 34, 37, 38, 39, 41, 2000, 3001, 42, 43, 45, 46, 48, 21, 49, 53, 55, 57, 56, 58, 60, 62, 63, 51, 65, 66, 67, 4004, 11103, 11102, 11104, 11105, 11106, 11107, 11108, 11121, 11141, 11142, 11143, 11201, 11226, 11227, 11221, 11225, 11224, 11222, 11223, 11161, 11181, 11241, 11261, 11301, 11321, 11322, 11323, 11324, 11325, 47, 3, 11101, 11109, 11110, 11111, 11112, 11281, 11326, 11341, 11361],
        # [0, 1, 2, 4, 7, 25, 31, 26, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 19, 20, 22, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009, 1010, 1011, 1012, 1013, 23, 27, 28, 30, 24, 31, 32, 33, 34, 37, 39, 41, 2000, 3001, 42, 43, 45, 46, 48, 21, 49, 53, 55, 57, 56, 58, 59, 60, 62, 63, 51, 65, 66, 67, 4004, 11103, 11102, 11104, 11105, 11106, 11107, 11108, 11121, 11141, 11142, 11143, 11201, 11226, 11227, 11221, 11225, 11224, 11222, 11223, 11161, 11181, 11241, 11261, 11301, 11321, 11322, 11323, 11324, 11325, 47, 3, 11101, 11109, 11110, 11111, 11112, 11281, 11326, 11341, 11361]
        #
        # # '1':
        #             [1013, 0, 1, 8, 1001, 1002, 1003, 1004, 1005, 1007, 25, 23, 26, 28, 31, 32, 33, 34, 37, 39, 35, 41, 3001, 43, 45, 48, 49, 53, 63, 51, 65, 60, 11101, 11106, 11105, 11107, 11110, 11109, 11121, 11181, 11102, 11103, 11201, 66, 66, 11281, 11302, 11303, 11304],
        #         # '4':
        #             [1013, 29, 0, 1, 8, 10, 1001, 1002, 1003, 1004, 1005, 1007, 29, 25, 26, 23, 27, 28, 31, 32, 33, 34, 31, 32, 33, 34, 37, 37, 39, 39, 41, 41, 38, 3001, 42, 43, 45, 60, 63, 51, 65, 11101, 11112, 11106, 11105, 11108, 11107, 11111, 11110, 11109, 11121, 11102, 11103, 11181, 11201, 66, 11281, 11302, 11303, 11304],
        #         # '5':
        #             [1013, 16, 0, 1, 8, 10, 11, 13, 14, 15, 1001, 1002, 1003, 29, 1004, 1005, 1007, 25, 26, 23, 27, 28, 31, 32, 33, 34, 31, 32, 33, 34, 37, 37, 38, 39, 39, 41, 41, 3001, 42, 43, 45, 46, 47, 60, 63, 51, 65, 11101, 11104, 11112, 11106, 11105, 11108, 11107, 11111, 11110, 11109, 11121, 11102, 11103, 11181, 11201, 11223, 11225, 11227, 11224, 11221, 11222, 11226, 66, 67, 11281, 11302, 11303, 11304, 11305, 20],
        #         # '10':
        #             [1013, 0, 1, 1001, 1002, 1003, 1004, 1005, 1007, 25, 23, 26, 27, 28, 31, 32, 33, 34, 35, 37, 39, 41, 8, 3001, 43, 45, 63, 65, 51, 11101, 11106, 11105, 11107, 11110, 11109, 11121, 60, 11181, 11103, 11102, 11201, 66, 66, 11281, 11302, 11303, 11304],
        #         # '21':
        #             [1013, 0, 1, 8, 1001, 1002, 1003, 1004, 1005, 1007, 25, 23, 26, 28, 31, 32, 33, 34, 37, 39, 35, 41, 3001, 43, 45, 27, 63, 51, 65, 11101, 11106, 11105, 11107, 11110, 11109, 11121, 60, 11181, 11102, 11103, 11201, 66, 66, 11281, 11302, 11303, 11304],
        #         # '95':
        #             [1013, 0, 41, 39, 37, 35, 34, 33, 32, 31, 28, 27, 26, 23, 25, 1007, 1005, 1004, 1003, 1002, 1001, 1, 1006, 41, 1010, 1011, 1012, 1005, 1002, 1001, 12, 8, 39, 37, 35, 34, 33, 29, 0, 1, 32, 31, 31, 26, 17, 25, 1013, 1007, 43, 43, 63, 51, 65, 11101, 11106, 11105, 11108, 11109, 11107, 11110, 11121, 60, 11181, 11102, 11103, 11201, 66, 11281, 11302, 11303, 11304]
    ]
    test_role_name_patterns = [ 'Копия 60',
        # 'Copy role 1 ', 'Copy role 4 ', 'Copy role 5 ',
        #                        'Copy role 10 ', 'Copy role 21 ', 'Copy role 95 '
                                ]
    test_role_name_postfixes = ['']

    for i in range(len(test_role_name_patterns)):
        for postfix in test_role_name_postfixes:
            with reporter.step(u'Создаем тестовую роль с необходимыми правами'):
                test_role_name_pattern = test_role_name_patterns[i] + postfix
                role_id = db.balance().sequence_nextval('S_ROLE_ID')
                role_name = '{role_name_ptn} {role_id}'.format(role_name_ptn=test_role_name_pattern, role_id=role_id)
                query = 'INSERT INTO t_role_name (id, name) VALUES (:role_id, :role_name)'

                db.balance().execute(query, {'role_id': role_id, 'role_name': role_name},
                                             descr='Создаем роль')
                add_permissions_to_role(role_id, perm_ids[i])
                print str(role_id) + test_role_name_pattern

create_client_restr_roles()

def set_role(passport_id, role_id):
    query = 'insert into t_role_user (passport_id, role_id) values ({uid}, {role_id})'.format(
                uid=passport_id, role_id=role_id)
    db.balance().execute(query)

def add_role_to_login():
    roles = [10256]
    logins = ['yndx-yuelyasheva']
    for i in range(len(logins)):
        uid = steps.api.medium().GetPassportByLogin(0, logins[i])['Uid']
        set_role(uid, roles[i])

# add_role_to_login()