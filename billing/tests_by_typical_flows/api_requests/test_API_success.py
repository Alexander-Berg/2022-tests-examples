# -*- coding: utf-8 -*-
import json

import pytest

from btestlib.utils import aDict
from apikeys import apikeys_api, apikeys_defaults
from apikeys.apikeys_steps import prepare_key, get_projects, prepare_tariff_changing, get_link_by_key
from btestlib import matchers as mtch, utils
from apikeys.tests_by_object_model.apikeys_object_model import Service

__author__ = 'ilya_knysh'



def prepare_data(connection, service, tariff, base_tariff):
    oper_uid, login, token, _, key, client_id, person_id = prepare_key(
        connection, service_cc=service.cc, apikeys_login_pool=apikeys_defaults.APIKEYS_LOGIN_POOL)
    project_id = get_projects(oper_uid)['_id']
    link_id = get_link_by_key(key)[0]['link_id']
    service_id = service.id
    mapkit_service_token = 'mapkit_456287b45f7f0cdd2d5bcbd1805291d4fe8c29e8'
    price = 20000

    return {'admin': apikeys_defaults.ADMIN, 'oper_uid': oper_uid, 'user_uid': oper_uid, 'login': login,
            'project_id': project_id, 'link_id': link_id, 'key': key, 'key_id': key, 'service_id': service_id,
            'service_token': token, 'tariff': tariff, 'base_tariff': base_tariff, 'client_id': client_id,
            'person_id': person_id, 'price': price, 'mapkit_service_token': mapkit_service_token}


def data_for_contractless_tariffs(connection):
    service = Service(cc='market')
    return prepare_data(connection, service, 'market_api_client_mini', 'market_api_client_base')


def data_for_contract_tariffs(connection):
    service = Service(cc='apimaps')
    return prepare_data(connection, service, 'apimaps_500k_yearprepay_2017', 'apimaps_free')


def get_test_data(scenario, connection):
    return data_for_contract_tariffs(connection) \
        if scenario.pop('contract', False) else data_for_contractless_tariffs(connection)


def api_success(scenario, data):
    scenario = aDict(scenario)
    request_params = json.loads(scenario.params.format(**data))
    if hasattr(scenario, 'precondition'):
        precondition_params = json.loads(scenario.precondition[1].format(**data))
        utils.wait_until2(scenario.precondition[0](**precondition_params), mtch.equal_to(200), timeout=2,
                          sleep_time=0.1)
    response = scenario.handle(**request_params)
    utils.check_that(response.status_code, mtch.equal_to(200))
    if hasattr(scenario, 'postcondition'):
        postcondition_params = json.loads(scenario.postcondition[1].format(**data))
        scenario.postcondition[0](**postcondition_params)


@pytest.mark.parametrize(
    'scenario',
    [
        # Test-case 0
        {'description': u'get_audit_trail',
         'handle': apikeys_api.BO.get_audit_trail,
         'params': '{{"oper_uid": {admin}, "login": "{login}", "key": "{key}"}}',
         },

        # Test-case 1
        {'description': u'list_keys',
         'handle': apikeys_api.BO.list_keys,
         'params': '{{"oper_uid": {admin}, "user_login": "{login}"}}',
         },

        # Test-case 2
        {'description': u'get_permissions',
         'handle': apikeys_api.BO.get_permissions,
         'params': '{{"oper_uid": {admin}}}',
         },

        # Test-case 3
        {'description': u'get_user_info',
         'handle': apikeys_api.BO.get_user_info,
         'params': '{{"oper_uid": {admin}, "user_uid": {user_uid}}}'
         },

        # Test-case 4
        {'description': u'key_usage_stat',
         'handle': apikeys_api.BO.key_usage_stat,
         'params': '{{"oper_uid": {admin}, "key_id": "{key}", "service_id": {service_id}}}'
         },

        # Test-case 5
        {'description': u'update_ban',
         'handle': apikeys_api.BO.update_ban,
         'params': '{{"oper_uid": {admin}, "key": "{key}", "service_id": {service_id}, "ban": true, "reason_id": 104}}',
         'postcondition': (apikeys_api.BO.update_ban,
                           '{{"oper_uid": {admin}, "key": "{key}", "service_id": {service_id}, "ban": false,'
                           ' "reason_id": 106}}'),
         },

        #todo need precondition
        # # Test-case 6
        # {'description': u'activate_key',
        #  'handle': apikeys_api.BO.activate_key,
        #  'params': '{{"oper_uid": {admin}, "key": "{key}", "service_id": {service_id}}}'
        #  },

        # Test-case 7
        {'description': u'create_key',
         'handle': apikeys_api.BO.create_key,
         'params': '{{"oper_uid": {admin}, "user_uid": {user_uid}}}'
         },

        # Test-case 8
        {'description': u'update_service_link',
         'handle': apikeys_api.BO.update_service_link,
         'params': '{{"oper_uid": {admin}, "key": "{key}", "service_id": {service_id}}}'
         },

        # Test-case 9
        {'description': u'push_tariffs_to_balance',
         'handle': apikeys_api.BO.push_tariffs_to_ballance,
         'params': '{{"oper_uid": {admin}}}'
         },

        # Test-case 10
        {'description': u'get_tariff_tree',
         'handle': apikeys_api.BO.get_tariff_tree,
         'params': '{{"oper_uid": {admin}}}'
         },

        # Test-case 11
        {'description': u'update_unblockable',
         'handle': apikeys_api.BO.update_unblockable,
         'params': '{{"oper_uid": {admin}, "key": "{key}", "service_id": {service_id}, "unblockable": true}}'
         },

        # Test-case 12
        {'description': u'get_client_from_balance',
         'handle': apikeys_api.BO.get_client_from_balance,
         'params': '{{"oper_uid": {admin}, "user_uid": {user_uid}}}'
         },

        # Test-case 13
        {'description': u'schedule_tariff_changing',
         'handle': apikeys_api.BO.schedule_tariff_changing,
         'params': '{{"oper_uid": {admin}, "key": "{key}", "service_id": {service_id}, "tariff": "{tariff}"}}'
         },
    ],
    ids=lambda x: x.get('description'))
@pytest.mark.good
@pytest.mark.smoke
def test_bo(scenario, db_connection):
    data = get_test_data(scenario, db_connection)
    api_success(scenario, data)


# ----------------------------------------------------------------------------------------------------------------------


@pytest.mark.parametrize(
    'scenario',
    [
        # Test-case 0
        {'description': u'create_key',
         'handle': apikeys_api.API.create_key,
         'params': '{{"service_token": "{service_token}", "user_uid": {user_uid}}}'
         },

        # Test-case 1
        {'description': u'check_key',
         'handle': apikeys_api.API.check_key,
         'params': '{{"service_token": "{service_token}", "key": "{key}", "user_ip": "127.0.0.1", "ip_v": 4}}',
         },

        # Test-case 2
        {'description': u'get_link_info',
         'handle': apikeys_api.API.get_link_info,
         'params': '{{"service_token": "{service_token}", "key": "{key}", "user_ip": "127.0.0.1", "ip_v": 4}}'
         },

        # Test-case 3
        {'description': u'update_counters',
         'handle': apikeys_api.API.update_counters,
         'params': '{{"service_token": "{service_token}", "key": "{key}", "counter_params": {{"total": 0}}}}'
         },

        # Test-case 4
        {'description': u'ban_key',
         'handle': apikeys_api.API.ban_key,
         'params': '{{"service_token": "{service_token}", "key": "{key}"}}',
         'postcondition': (apikeys_api.BO.update_ban,
                           '{{"oper_uid": {admin}, "key": "{key}", "service_id": "{service_id}", "ban": false,'
                           ' "reason_id": 106}}'),
         },

        # Test-case 5
        {'description': u'project_service_link_export',
         'handle': apikeys_api.API2.project_service_link_export,
         'params': '{{"service_token": "{mapkit_service_token}"}}'
         },
    ],
    ids=lambda x: x.get('description'))
@pytest.mark.good
@pytest.mark.smoke
def test_api(scenario, db_connection):
    data = get_test_data(scenario, db_connection)
    api_success(scenario, data)


# ----------------------------------------------------------------------------------------------------------------------


# @pytest.mark.parametrize(
#     'scenario',
#     [
#         # Test-case 0
#         {'description': u'get_user_info',
#          'handle': apikeys_api.UI.get_user_info,
#          'params': '{{"user_uid": {user_uid}}}',
#          },
#
#         # Test-case 1
#         {'description': u'create_key',
#          'handle': apikeys_api.UI.create_key,
#          'params': '{{"oper_uid": {oper_uid}}}',
#          },
#
#         # Test-case 2
#         {'description': u'update_key',
#          'handle': apikeys_api.UI.update_key,
#          'params': '{{"oper_uid": {oper_uid}, "key_id": "{key}", "name": "k1"}}'
#          },
#
#         # Test-case 3
#         {'description': u'update_active',
#          'handle': apikeys_api.UI.update_active,
#          'params': '{{"oper_uid": {oper_uid}, "key_id": "{key}", "service_id": {service_id}, "active": false}}',
#          'postcondition': (
#                  apikeys_api.UI.update_active,
#                  '{{"oper_uid": {oper_uid}, "key_id": "{key}", "service_id": {service_id}, "active": true}}'),
#          },
#
#         # Test-case 4
#         {'description': u'update_person',
#          'handle': apikeys_api.UI.update_person,
#          'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": {service_id},'
#                    ' "balance_person_id": "{person_id}"}}'
#          },
#
#         # Test-case 5
#         {'description': u'list_project_links_financial',
#          'handle': apikeys_api.UI.list_project_links_financial,
#          'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}"}}'
#          },
#
#         # Test-case 6
#         {'description': u'list_keys',
#          'handle': apikeys_api.UI.list_keys,
#          'params': '{{"oper_uid": {oper_uid}}}'
#          },
#
#         # # APIKEYS-922
#         # # Test-case 7
#         # {'description': u'key_blocks',
#         #  'handle': apikeys_api.UI.key_blocks,
#         #  'params': '{{"oper_uid": {oper_uid}}}'
#         #  },
#
#         # отсутствуют тарифы, которые необходимо включать
#         # Test-case 8
#         # {'description': u'turn_on_tariff',
#         #  'handle': apikeys_api.UI.turn_on_tariff,
#         #  'precondition': (prepare_turn_on_tariff, '{{"oper_uid": {oper_uid}, "project_id": "{project_id}",'
#         #                                           ' "service_id": {service_id}, "tariff_cc": "{tariff}",'
#         #                                           '"person_id": {person_id}, "key": "{key}", "client_id": {client_id},'
#         #                                           ' "price": {price}}}'),
#         #  'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": {service_id},'
#         #            ' "tariff_cc": "{tariff}", "amount": 1}}',
#         #  'postcondition': (apikeys_api.BO.schedule_tariff_changing,
#         #                    '{{"oper_uid": {admin}, "key": "{key}", "service_id": {service_id},'
#         #                    ' "tariff": "{base_tariff}"}}')
#         #  },
#
#         # Test-case 9
#         {'description': u'get_request_deposit_personal_account',
#          'handle': apikeys_api.UI.get_request_deposit_personal_account,
#          'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": {service_id},'
#                    ' "amount": 5000}}',
#          'precondition': (prepare_tariff_changing,
#                           '{{"tariff": "{tariff}", "oper_uid": {oper_uid}, "project_id": "{project_id}",'
#                           ' "service_id": {service_id},"person_id": {person_id}, "key": "{key}"}}'),
#          },
#
#         # Test-case 10
#         # {'description': u'cancel_tariff_changing',
#         #  'handle': apikeys_api.UI.cancel_tariff_changing,
#         #  'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": {service_id}}}',
#         #  'precondition': (apikeys_api.BO.schedule_tariff_changing,
#         #                   '{{"oper_uid": {admin}, "key": "{key}", "service_id": {service_id},'
#         #                   ' "tariff": "{base_tariff}"}}')
#         #  },
#         # Test-case 11
#         {'description': u'update_service_link',
#          'handle': apikeys_api.UI.update_service_link,
#          'params': '{{"oper_uid": {oper_uid}, "key_id": "{key}", "service_id": 27}}',
#          'postcondition': (
#                  apikeys_api.BO.update_service_link,
#                  '{{"oper_uid": {admin}, "key": "{key}", "service_id": 15, "method": "PATCH"}}')
#          },
#
#         # Test-case 12
#         {'description': u'get_permissions',
#          'handle': apikeys_api.UI.get_permissions,
#          'params': '{{"oper_uid": {admin}}}',
#          },
#     ],
#     ids=lambda x: x.get('description'))
# @pytest.mark.good
# @pytest.mark.smoke
# def test_ui(scenario, db_connection):
#     data = get_test_data(scenario, db_connection)
#     api_success(scenario, data)


# ----------------------------------------------------------------------------------------------------------------------


@pytest.mark.parametrize(
    'scenario',
    [
        # Test-case 0
        {'description': u'run_contractor',
         'handle': apikeys_api.TEST.run_user_contractor,
         'params': '{{"user_uid": {user_uid}}}'
         },

        # Test-case 1
        {'description': u'run_limit_checker',
         'handle': apikeys_api.TEST.run_limit_checker,
         'params': '{{"link_id": "{link_id}"}}'
         },

        # Test-case 2
        {'description': u'run_tarifficator',
         'handle': apikeys_api.TEST.run_tarifficator,
         'params': '{{"link_id": "{link_id}"}}'
         }
    ],
    ids=lambda x: x.get('description'))
@pytest.mark.good
@pytest.mark.smoke
def test_test_serv(scenario, db_connection):
    data = get_test_data(scenario, db_connection)
    api_success(scenario, data)

# ----------------------------------------------------------------------------------------------------------------------


@pytest.mark.parametrize(
    'scenario',
    [
        # # Test-case 0
        # {'description': u'service_list',
        #  'handle': apikeys_api.UI2.service_list,
        #  'params': '{{}}',
        #  },
        #
        # # Test-case 1
        # {'description': u'project_list',
        #  'handle': apikeys_api.UI2.project_list,
        #  'params': '{{"oper_uid": {oper_uid}}}',
        #  },
        #
        # # Test-case 2
        # {'description': u'project_service_link_list',
        #  'handle': apikeys_api.UI2.project_service_link_list,
        #  'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}"}}'
        #  },
        #
        # # Test-case 3
        # {'description': u'project_service_link_details',
        #  'handle': apikeys_api.UI2.project_service_link_details,
        #  'params': '{{"project_id": "{project_id}", "oper_uid": {oper_uid},"service_id": {service_id}}}'
        #  },
        #
        # # # Test-case 4
        # # {'description': u'project_service_link_update',
        # #  'handle': apikeys_api.UI2.project_service_link_update,
        # #  'params': '{{"project_id": "{project_id}", "oper_uid": {oper_uid},"service_id": {service_id}}}'
        # #  },
        #
        # # Test-case 5
        # {'description': u'key_list',
        #  'handle': apikeys_api.UI2.key_list,
        #  'params': '{{"project_id":"{project_id}", "oper_uid": {oper_uid}, "service_id": {service_id}}}'
        #  },
        #
        # # Test-case 6
        # {'description': u'key_details',
        #  'handle': apikeys_api.UI2.key_details,
        #  'params': '{{"project_id":"{project_id}", "oper_uid": {oper_uid}, "service_id": {service_id}, "key": "{key}"}}'
        #  },
        #
        # # # Test-case 7
        # # {'description': u'key_update',
        # #  'handle': apikeys_api.UI2.key_update,
        # #  'params': '{{"project_id":"{project_id}", "oper_uid": {oper_uid}, "service_id": {service_id}, "key": "{key}"}}'
        # #  },

        # Test-case 8
        {'description': u'create_balance_contract',
         'handle': apikeys_api.UI2.create_balance_contract,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": {service_id}, '
                   '"person": "{person_id}", "tariff": "{tariff}"}}',
         'contract': True,
         },
    ],
    ids=lambda x: x.get('description'))
@pytest.mark.good
@pytest.mark.smoke
def test_ui2(scenario, db_connection):
    data = get_test_data(scenario, db_connection)
    api_success(scenario, data)

@pytest.mark.parametrize(
    'scenario',
    [

        # Test-case 0
        {'description': u'create_startrek_issue',
         'handle': apikeys_api.ST_ISSUE.create_startrek_issue,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": {service_id}, "new_tariff": "{tariff}"}}',
         'contract': True,
         },
    ],
    ids=lambda x: x.get('description'))

@pytest.mark.smoke
#todo сделать механизм закрытия задачи в очереди ST
def test_startreck_issue(scenario,db_connection):
    data = get_test_data(scenario,db_connection)
    api_success(scenario, data)
