# -*- coding: utf-8 -*-
import json

import pytest
from collections import namedtuple
from btestlib.utils import aDict
from apikeys import apikeys_api, apikeys_defaults
from btestlib import matchers as mtch, utils
from apikeys.apikeys_steps import prepare_key, get_projects, prepare_tariff_changing, prepare_turn_on_tariff, \
    get_link_by_key

__author__ = 'kostya-karpus'

Expected = namedtuple('Expected', ['status_code', 'content'])


@pytest.fixture(scope='module')
def test_data():
    service_cc = 'market'
    oper_uid, login, token, service_id, key, client_id, person_id = prepare_key(
        free_passport=0, service_cc=service_cc, apikeys_login_pool=apikeys_defaults.APIKEYS_LOGIN_POOL_ERROR_TEST)
    project_id = get_projects(oper_uid)['_id']
    link_id = get_link_by_key(key)[0]['link_id']
    mapkit_service_token = 'mapkit_456287b45f7f0cdd2d5bcbd1805291d4fe8c29e8'
    tariff = 'market_vendor_mini'
    price = 20000
    service_token_1 = 'apimapsplus_b85e70c74ca130fdc09ddce2516e516119b39e5d'
    service_cc_1 = 'apimapsplus'
    tariff_1 = 'apimaps_1000_yearprepay_2017'
    oper_uid_1, _, _, service_id_1, key_1, _, _ = prepare_key(
        free_passport=1, service_cc=service_cc_1, apikeys_login_pool=apikeys_defaults.APIKEYS_LOGIN_POOL_ERROR_TEST)
    project_id_1 = get_projects(oper_uid_1)['_id']
    return {'admin': apikeys_defaults.ADMIN, 'oper_uid': oper_uid, 'user_uid': oper_uid, 'login': login,
            'project_id': project_id, 'link_id': link_id, 'key': key, 'key_id': key, 'service_id': service_id,
            'service_token': token, 'tariff': tariff, 'client_id': client_id, 'person_id': person_id, 'price': price,
            'mapkit_service_token': mapkit_service_token, 'oper_uid_1': oper_uid_1, 'service_id_1': service_id_1,
            'key_1': key_1, 'service_token_1': service_token_1, 'tariff_1': tariff_1, 'project_id_1': project_id_1}


class Error(object):
    api_service_not_found = '{{"error": "Service not found"}}'
    api_mandatory = '''{{"error": "procedure '%s': parameter[0]: procedure '%s': parameter[0]: hash sitem '%s' is mandatory"}}'''
    api_key_not_found_object = '''{{"error": "Object not found: object Key({{'_id': u'%s'}})"}}'''
    api_key_not_found = '{{"error": "Key not found"}}'
    api_key_not_attached = '{{"error": "Key is not attached to the service"}}'
    api_key_not_active = '{{"error": "Key is not active"}}'
    api_key_already_banned = '{{"error": "Key is already banned"}}'
    api_invalid_user_uid = '''{{"error": "procedure '%s': parameter[0]: hash item 'user_uid': procedure '%s': parameter[0]: hash item 'user_uid': invalid number value: u'test'"}}'''
    api_link_not_found = '''{{"error": "Object not found: object ProjectServiceLink({{'service_id': {service_id_1}, 'hidden': {{'$ne': True}}, 'project_id': u'{project_id}'}})"}}'''
    api_without_unit = '{{"error": "Must provide one of the units"}}'
    api_invalid_token_header = '{{"error": "Insufficient rights"}}'
    api_500_invalid_optional_param = '{{"error": "Internal Server Error"}}'
    api_invalid_date_format = '{{"error": \"[\'update_dt__gt must be Date value in ISO format\']\"}}'

    ui_mandatory = '''{{"message": "procedure '%s': parameter[0]: procedure '%s': parameter[0]: hash sitem '%s' is mandatory", "statusCode": "Bad Request"}}'''
    ui_invalid_oper_uid = '''{{"message": "procedure '%s': parameter[0]: hash item 'oper_uid': procedure '%s': parameter[0]: hash item 'oper_uid': invalid operator uid value: u'test'", "statusCode": "Bad Request"}}'''
    ui_forbidden = '{{"message": "Insufficient rights", "statusCode": "Forbidden"}}'
    ui_key_id_not_found = '''{{"message": "Object not found: object Key({{'hidden': {{'$ne': True}}, '_id': u'%s'}})", "statusCode": "Not Found"}}'''
    ui_already_active = '{{"message": "Already active", "statusCode": "Bad Request"}}'
    ui_invalid_service_id = '''{{"message": "Object not found: object KeyServiceConfig({{'service_id': 31, 'hidden': {{'$ne': True}}, 'key': u'{key}'}})", "statusCode": "Not Found"}}'''
    ui_invalid_project_id = '''{{"message": "Object not found: object Project({{'hidden': {{'$ne': True}}, '_id': u'%s'}})", "statusCode": "Not Found"}}'''
    ui_service_id_not_found = '{{"message": "Service not found", "statusCode": "Not Found"}}'
    ui_service_id_not_in_project = '''{{"message": "Object not found: object Service({{'_id': 777}})", "statusCode": "Not Found"}}'''
    ui_person_not_found = '{{"message": "Person not found", "statusCode": "Not Found"}}'
    ui_invalid_number_value = '''{{"message": "procedure '%s': parameter[0]: hash item '%s': procedure '%s': parameter[0]: hash item '%s': invalid number value: u'test'", "statusCode": "Bad Request"}}'''
    ui_invalid_date = '''{{"message": "procedure '%s': parameter[0]: hash item '%s': procedure '%s': parameter[0]: hash item '%s': invalid date string: 'test'", "statusCode": "Bad Request"}}'''
    ui_mandatory_key_id_or_project_id = '{{"message": "key_id or project_id is mandatory", "statusCode": "Bad Request"}}'
    ui_attach_person = '{{"message": "Attach person or contract fist", "statusCode": "Bad Request"}}'
    ui_500_get_request = r'''{{"message": "Error: BalanceError\nDescription: BalanceError(\"<Fault -1: '<error><msg>Invalid parameter for function: qty or quantity must be more than 0</msg><wo-rollback>0</wo-rollback><method>Balance.CreateRequest2</method><code>INVALID_PARAM</code><parent-codes><code>EXCEPTION</code></parent-codes><contents>Invalid parameter for function: qty or quantity must be more than 0</contents></error>'>\",)\nTraceback:\n  File \"./apikeys/apikeys_servant.py\", line 256, in process_request\n    return super(JsonUIResult, self).process_request(env_helper)\n  File \"./apikeys/apikeys_servant.py\", line 243, in process_request\n    res = super(JsonResultHttp, self).process_request(env_helper)\n  File \"./apikeys/apikeys_servant.py\", line 233, in process_request\n    return self.get_method(path_name)(query_params)\n  File \"/apikeys/apikeys/mapper/context.py\", line 12, in wrapper\n    return func(*args, **kwargs)\n  File \"/apikeys/apikeys/mapper/context.py\", line 12, in wrapper\n    return func(*args, **kwargs)\n  File \"/usr/lib/python2.7/dist-packages/butils/rpcutil.py\", line 30, in f\n    xrange(len(args)))\n  File \"/apikeys/apikeys/logic_impl.py\", line 28, in wrapper\n    return f(args[0], user, *args[1:], **kwargs)\n  File \"/apikeys/apikeys/uilogic.py\", line 32, in wrapper\n    return handle(self, oper, project, params)\n  File \"/apikeys/apikeys/uilogic.py\", line 284, in get_request_deposit_personal_account\n    request_url = b2a.create_request_deposit_personal_account(oper, pa_order, tariff, params['amount'])\n  File \"/apikeys/apikeys/balance2apikeys.py\", line 195, in create_request_deposit_personal_account\n    firm_id=tariff.personal_account.firm_id)\n  File \"/apikeys/apikeys/balalance_wrapper/balance.py\", line 337, in create_request\n    return self._call_rpc('CreateRequest2', operator_uid, client_id, orders, props)\n  File \"/apikeys/apikeys/balalance_wrapper/balance.py\", line 90, in _call_rpc\n    raise self.BalanceError('%s' % (e,))\n", "statusCode": "Internal Server Error"}}'''
    ui_500_turn_on = '''{{"message": "Error: BalanceError\nDescription: BalanceError(\"<Fault -1: '<error><msg>Invalid parameter for function: qty or quantity must be more than 0</msg><wo-rollback>0</wo-rollback><method>Balance.CreateRequest2</method><code>INVALID_PARAM</code><parent-codes><code>EXCEPTION</code></parent-codes><contents>Invalid parameter for function: qty or quantity must be more than 0</contents></error>'>\",)\nTraceback:\n  File \"./apikeys/apikeys_servant.py\", line 256, in process_request\n    return super(JsonUIResult, self).process_request(env_helper)\n  File \"./apikeys/apikeys_servant.py\", line 243, in process_request\n    res = super(JsonResultHttp, self).process_request(env_helper)\n  File \"./apikeys/apikeys_servant.py\", line 233, in process_request\n    return self.get_method(path_name)(query_params)\n  File \"/apikeys/apikeys/mapper/context.py\", line 12, in wrapper\n    return func(*args, **kwargs)\n  File \"/apikeys/apikeys/mapper/context.py\", line 12, in wrapper\n    return func(*args, **kwargs)\n  File \"/usr/lib/python2.7/dist-packages/butils/rpcutil.py\", line 30, in f\n    xrange(len(args)))\n  File \"/apikeys/apikeys/logic_impl.py\", line 28, in wrapper\n    return f(args[0], user, *args[1:], **kwargs)\n  File \"/apikeys/apikeys/uilogic.py\", line 32, in wrapper\n    return handle(self, oper, project, params)\n  File \"/apikeys/apikeys/uilogic.py\", line 308, in turn_on_tariff\n    b2a.turn_on_tariff(link, tariff, params['amount'])\n  File \"/apikeys/apikeys/balance2apikeys.py\", line 217, in turn_on_tariff\n    0, user.get_client_id(), orders, firm_id=tariff.personal_account.firm_id)\n  File \"/apikeys/apikeys/balalance_wrapper/balance.py\", line 337, in create_request\n    return self._call_rpc('CreateRequest2', operator_uid, client_id, orders, props)\n  File \"/apikeys/apikeys/balalance_wrapper/balance.py\", line 90, in _call_rpc\n    raise self.BalanceError('%s' % (e,))\n", "statusCode": "Internal Server Error"}}'''
    ui_500_not_implemented = r'''{{"message": "Error: NotImplementedError\nDescription: \nTraceback:\n  File \"./apikeys/apikeys_servant.py\", line 256, in process_request\n    return super(JsonUIResult, self).process_request(env_helper)\n  File \"./apikeys/apikeys_servant.py\", line 243, in process_request\n    res = super(JsonResultHttp, self).process_request(env_helper)\n  File \"./apikeys/apikeys_servant.py\", line 233, in process_request\n    return self.get_method(path_name)(query_params)\n  File \"/apikeys/apikeys/mapper/context.py\", line 12, in wrapper\n    return func(*args, **kwargs)\n  File \"/apikeys/apikeys/mapper/context.py\", line 12, in wrapper\n    return func(*args, **kwargs)\n  File \"/usr/lib/python2.7/dist-packages/butils/rpcutil.py\", line 30, in f\n    xrange(len(args)))\n  File \"/apikeys/apikeys/logic_impl.py\", line 28, in wrapper\n    return f(args[0], user, *args[1:], **kwargs)\n  File \"/apikeys/apikeys/uilogic.py\", line 32, in wrapper\n    return handle(self, oper, project, params)\n  File \"/apikeys/apikeys/uilogic.py\", line 306, in turn_on_tariff\n    raise NotImplementedError\n", "statusCode": "Internal Server Error"}}'''
    ui_not_enough = '{{"message": "NotEnoughFunds", "statusCode": "Payment Required"}}'
    ui_tariff_not_found = '''{{"message": "Object not found: object Tariff({{'cc': u'test'}})", "statusCode": "Not Found"}}'''
    ui_tariff_is_not_attachable = '{{"message": "Tariff is not attachable", "statusCode": "Bad Request"}}'
    ui_tariff_is_not_scheduled = '{{"message": "There is no scheduled tariff", "statusCode": "Bad Request"}}'
    ui_tariff_settings_is_broken = '{{"message": "Wrong Tariff in link or Tariff settings is broken", "statusCode": "Bad Request"}}'

    bo_must_be_provided = '{{"message": "%s must be provided", "statusCode": "Bad Request"}}'
    bo_bad_service_list = '{{"message": "Bad service list", "statusCode": "Bad Request"}}'
    bo_user_not_found = '''{{"message": "Object not found: object User({{'login': u'nigol-dilavni-tset'}})", "statusCode": "Not Found"}}'''
    bo_500_invalid_literal = r'''{{"message": "Error: ValueError\nDescription: invalid literal for int() with base 10: 'test'\nTraceback:\n  File \"./apikeys/apikeys_servant.py\", line 256, in process_request\n    return super(JsonUIResult, self).process_request(env_helper)\n  File \"./apikeys/apikeys_servant.py\", line 243, in process_request\n    res = super(JsonResultHttp, self).process_request(env_helper)\n  File \"./apikeys/apikeys_servant.py\", line 233, in process_request\n    return self.get_method(path_name)(query_params)\n  File \"/usr/lib/python2.7/dist-packages/butils/rpcutil.py\", line 30, in f\n    xrange(len(args)))\n  File \"/apikeys/apikeys/logic_impl.py\", line 28, in wrapper\n    return f(args[0], user, *args[1:], **kwargs)\n  File \"/apikeys/apikeys/bologic.py\", line 49, in wrapper\n    return f(*args, **kwargs)\n  File \"/apikeys/apikeys/bologic.py\", line 149, in get_audit_trail\n    'audit_trail': [at.serialize() for at in logic_impl.paged_query(q, ps, pn)],\n  File \"/usr/lib/python2.7/dist-packages/mongoengine-0.10.6-py2.7.egg/mongoengine/queryset/queryset.py\", line 80, in _iter_results\n    self._populate_cache()\n  File \"/usr/lib/python2.7/dist-packages/mongoengine-0.10.6-py2.7.egg/mongoengine/queryset/queryset.py\", line 92, in _populate_cache\n    self._result_cache.append(self.next())\n  File \"/usr/lib/python2.7/dist-packages/mongoengine-0.10.6-py2.7.egg/mongoengine/queryset/base.py\", line 1407, in next\n    raw_doc = self._cursor.next()\n  File \"/usr/lib/python2.7/dist-packages/mongoengine-0.10.6-py2.7.egg/mongoengine/queryset/base.py\", line 1481, in _cursor\n    self._cursor_obj = self._collection.find(self._query,\n  File \"/usr/lib/python2.7/dist-packages/mongoengine-0.10.6-py2.7.egg/mongoengine/queryset/base.py\", line 1515, in _query\n    self._mongo_query = self._query_obj.to_query(self._document)\n  File \"/usr/lib/python2.7/dist-packages/mongoengine-0.10.6-py2.7.egg/mongoengine/queryset/visitor.py\", line 90, in to_query\n    query = query.accept(QueryCompilerVisitor(document))\n  File \"/usr/lib/python2.7/dist-packages/mongoengine-0.10.6-py2.7.egg/mongoengine/queryset/visitor.py\", line 137, in accept\n    self.children[i] = self.children[i].accept(visitor)\n  File \"/usr/lib/python2.7/dist-packages/mongoengine-0.10.6-py2.7.egg/mongoengine/queryset/visitor.py\", line 155, in accept\n    return visitor.visit_query(self)\n  File \"/usr/lib/python2.7/dist-packages/mongoengine-0.10.6-py2.7.egg/mongoengine/queryset/visitor.py\", line 78, in visit_query\n    return transform.query(self.document, **query.query)\n  File \"/usr/lib/python2.7/dist-packages/mongoengine-0.10.6-py2.7.egg/mongoengine/queryset/transform.py\", line 95, in query\n    value = field.prepare_query_value(op, value)\n  File \"/usr/lib/python2.7/dist-packages/mongoengine-0.10.6-py2.7.egg/mongoengine/fields.py\", line 210, in prepare_query_value\n    return super(IntField, self).prepare_query_value(op, int(value))\n", "statusCode": "Internal Server Error"}}'''
    bo_invalid_number_value = '''{{"message": "procedure '%s': parameter[0]: hash item '%s': procedure '%s': parameter[0]: hash item '%s': invalid number value: u'test'", "statusCode": "Bad Request"}}'''
    bo_yandex_passport_not_found = '{{"message": "Object not found: Yandex.Passport not found for: {oper_uid}", "statusCode": "Not Found"}}'
    bo_already_approved = '{{"message": "Key {key} is already approved for service {service_id}", "statusCode": "Bad Request"}}'
    bo_object_not_found = '''{{"message": "Object not found: object %s({{'%s': %s}})", "statusCode": "Not Found"}}'''
    bo_bad_reason = '{{"message": "Bad reason id 10000 for service {service_id_1}", "statusCode": "Bad Request"}}'
    bo_already_banned = '{{"message": "Key is already banned", "statusCode": "Bad Request"}}'
    bo_key_id_is_mandatory = '{{"message": "key_id is mandatory", "statusCode": "Bad Request"}}'
    bo_link_not_found = '{{"message": "Link not found", "statusCode": "Not Found"}}'
    bo_already_applied = '{{"message": "Already applied", "statusCode": "Bad Request"}}'
    bo_service_id_mandatory = '''{{"message": "procedure 'update_unblockable': parameter[0]: procedure 'update_unblockable': parameter[0]: hash sitem 'service_id' is mandatory", "statusCode": "Bad Request"}}'''
    bo_oper_uid_is_mandatory = '''{{"message": "procedure 'update_unblockable': parameter[0]: procedure 'update_unblockable': parameter[0]: hash sitem 'oper_uid' is mandatory", "statusCode": "Bad Request"}}'''
    bo_invalid_service_id = '''{{"message": "Object not found: object Service({{'_id': 777}})", "statusCode": "Not Found"}}'''
    bo_invalid_key_id = '''{{"message": "Object not found: object Key({{'_id': u'%s'}})", "statusCode": "Not Found"}}'''
    bo_not_attachable = '{{"message": "Tariff is not attachable", "statusCode": "Bad Request"}}'
    bo_forbidden = '{{"message": "Insufficient rights", "statusCode": "Forbidden"}}'
    bo_key_id_is_mandatory_unblockable = '''{{"message": "procedure 'update_unblockable': parameter[0]: procedure 'update_unblockable': parameter[0]: hash sitem 'key' is mandatory", "statusCode": "Bad Request"}}'''
    bo_unblockable_is_mandatory = '''{{"message": "procedure 'update_unblockable': parameter[0]: procedure 'update_unblockable': parameter[0]: hash sitem 'unblockable' is mandatory", "statusCode": "Bad Request"}}'''


def api_errors(scenario, test_data):
    scenario = aDict(scenario)
    request_params = json.loads(scenario.params.format(**test_data))
    request_params['allow_not_200'] = 'true'
    if hasattr(scenario, 'precondition'):
        precondition_params = json.loads(scenario.precondition[1].format(**test_data))
        utils.wait_until2(scenario.precondition[0](**precondition_params), mtch.equal_to(200), timeout=2,
                          sleep_time=0.1)
    response = scenario.handle(**request_params)
    utils.check_that(response.status_code, mtch.equal_to(scenario.expected.status_code))
    if scenario.expected.status_code != 500:
        expected = scenario.expected.content.format(**test_data)
        utils.check_that(response.content, mtch.equal_to(expected))
    if hasattr(scenario, 'postcondition'):
        postcondition_params = json.loads(scenario.postcondition[1].format(**test_data))
        scenario.postcondition[0](**postcondition_params)


@pytest.mark.parametrize(
    'scenario',
    [
        # create_key
        # Test-case 0
        {'description': u'invalid service_token',
         'handle': apikeys_api.API.create_key,
         'params': '{{"service_token": "test", "user_uid": {user_uid}}}',
         'expected': Expected(404, Error.api_service_not_found)
         },

        # Test-case 1
        {'description': u'invalid user_uid',
         'handle': apikeys_api.API.create_key,
         'params': '{{"service_token": "{service_token}", "user_uid": "test"}}',
         'expected': Expected(400, Error.api_invalid_user_uid % ('create_key', 'create_key'))
         },

        # Test-case 2
        {'description': u'invalid service_token and user_uid',
         'handle': apikeys_api.API.create_key,
         'params': '{{"service_token": 1, "user_uid": "test"}}',
         'expected': Expected(400, Error.api_invalid_user_uid % ('create_key', 'create_key'))
         },

        # Test-case 3
        {'description': u'without user_uid',
         'handle': apikeys_api.API.create_key,
         'params': '{{"user_uid": null, "service_token": "{service_token}"}}',
         'expected': Expected(400, Error.api_mandatory % ('create_key', 'create_key', 'user_uid'))
         },

        # Test-case 4
        {'description': u'without service_token',
         'handle': apikeys_api.API.create_key,
         'params': '{{"user_uid": {user_uid}, "service_token": null}}',
         'expected': Expected(400, Error.api_mandatory % ('create_key', 'create_key', 'service_token'))
         },

        # check_key
        # Test-case 5
        {'description': u'invalid key',
         'handle': apikeys_api.API.check_key,
         'params': '{{"service_token": "{service_token}", "key": "test", "user_ip": "127.0.0.1", "ip_v": 4}}',
         'expected': Expected(404, Error.api_key_not_found)
         },

        # Test-case 6
        {'description': u'invalid service_token',
         'handle': apikeys_api.API.check_key,
         'params': '{{"service_token": "test", "key": "{key}", "user_ip": "127.0.0.1", "ip_v": 6}}',
         'expected': Expected(404, Error.api_service_not_found)
         },

        # Test-case 7
        {'description': u'key is not attached to the service',
         'handle': apikeys_api.API.check_key,
         'params': '{{"service_token": "{mapkit_service_token}", "key": "{key}","user_ip": "127.0.0.1", "ip_v": 4}}',
         'expected': Expected(403, Error.api_key_not_attached)
         },

        # Test-case 8
        {'description': u'key is not active',
         'handle': apikeys_api.API.check_key,
         'params': '{{"service_token": "{service_token_1}", "key": "{key_1}","user_ip": "127.0.0.1", "ip_v": 4}}',
         'expected': Expected(403, Error.api_key_not_active)
         },

        # Test-case 9
        {'description': u'without user_ip',
         'handle': apikeys_api.API.check_key,
         'params': '{{"service_token": "{service_token}", "key": "{key}", "user_ip": null, "ip_v": 4}}',
         'expected': Expected(400, Error.api_mandatory % ('check_key', 'check_key', 'user_ip'))
         },

        # Test-case 10
        {'description': u'without service_token',
         'handle': apikeys_api.API.check_key,
         'params': '{{"service_token": null, "key": "{key}", "user_ip": "127.0.0.1", "ip_v": 4}}',
         'expected': Expected(400, Error.api_mandatory % ('check_key', 'check_key', 'service_token'))
         },

        # Test-case 11
        {'description': u'without key',
         'handle': apikeys_api.API.check_key,
         'params': '{{"service_token": "{service_token}", "key": null, "user_ip": "127.0.0.1", "ip_v": 4}}',
         'expected': Expected(400, Error.api_mandatory % ('check_key', 'check_key', 'key'))
         },

        # get_link_info
        # Test-case 12
        {'description': u'invalid key',
         'handle': apikeys_api.API.get_link_info,
         'params': '{{"service_token": "{service_token}", "key": "test", "user_ip": "127.0.0.1", "ip_v": 4}}',
         'expected': Expected(404, Error.api_key_not_found)
         },

        # Test-case 13
        {'description': u'invalid service_token',
         'handle': apikeys_api.API.get_link_info,
         'params': '{{"service_token": "test", "key": "{key}", "user_ip": "127.0.0.1", "ip_v": 6}}',
         'expected': Expected(404, Error.api_service_not_found)
         },

        # Test-case 14
        {'description': u'object not found',
         'handle': apikeys_api.API.get_link_info,
         'params': '{{"service_token": "{service_token_1}", "key": "{key}", "user_ip": "127.0.0.1", "ip_v": 6}}',
         'expected': Expected(404, Error.api_link_not_found)
         },

        # Test-case 15
        {'description': u'without user_ip',
         'handle': apikeys_api.API.get_link_info,
         'params': '{{"service_token": "{service_token}", "key": "{key}", "user_ip": null, "ip_v": 4}}',
         'expected': Expected(400, Error.api_mandatory % ('get_link_info', 'get_link_info', 'user_ip'))
         },

        # Test-case 16
        {'description': u'without service_token',
         'handle': apikeys_api.API.get_link_info,
         'params': '{{"service_token": null, "key": "{key}", "user_ip": "127.0.0.1", "ip_v": 4}}',
         'expected': Expected(400, Error.api_mandatory % ('get_link_info', 'get_link_info', 'service_token'))
         },

        # Test-case 17
        {'description': u'without key',
         'handle': apikeys_api.API.get_link_info,
         'params': '{{"service_token": "{service_token}", "key": null, "user_ip": "127.0.0.1", "ip_v": 4}}',
         'expected': Expected(400, Error.api_mandatory % ('get_link_info', 'get_link_info', 'key'))
         },

        # update_counters
        # Test-case 18
        {'description': u'without unit',
         'handle': apikeys_api.API.update_counters,
         'params': '{{"service_token": "{service_token}", "key": "{key}", "counter_params": {{"light_hits": null}}}}',
         'expected': Expected(400, Error.api_without_unit)
         },

        # Test-case 19
        {'description': u'invalid key',
         'handle': apikeys_api.API.update_counters,
         'params': '{{"service_token": "{service_token}", "key": "test", "counter_params": {{"light_hits": 0}}}}',
         'expected': Expected(404, Error.api_key_not_found_object % 'test')
         },

        # Test-case 20
        {'description': u'invalid service_token',
         'handle': apikeys_api.API.update_counters,
         'params': '{{"service_token": "test", "key": "{key}", "counter_params": {{"hits": 0}}}}',
         'expected': Expected(404, Error.api_service_not_found)
         },

        # Test-case 21
        {'description': u'without service_token',
         'handle': apikeys_api.API.update_counters,
         'params': '{{"service_token": null, "key": "{key}", "counter_params": {{"hits": 0}}}}',
         'expected': Expected(400, Error.api_mandatory % ('update_counters', 'update_counters', 'service_token'))
         },

        # Test-case 22
        {'description': u'without key',
         'handle': apikeys_api.API.update_counters,
         'params': '{{"service_token": "{service_token}", "key": null, "counter_params": {{"hits": 0}}}}',
         'expected': Expected(400, Error.api_mandatory % ('update_counters', 'update_counters', 'key'))
         },

        # ban_key
        # Test-case 23
        {'description': u'invalid service',
         'handle': apikeys_api.API.ban_key,
         'params': '{{"service_token": "test", "key": "{key}"}}',
         'expected': Expected(404, Error.api_service_not_found)
         },

        # Test-case 24
        {'description': u'invalid key',
         'handle': apikeys_api.API.ban_key,
         'params': '{{"service_token": "{service_token}", "key": "test"}}',
         'expected': Expected(404, Error.api_key_not_found_object % 'test')
         },

        # Test-case 24
        {'description': u'already banned',
         'precondition': (apikeys_api.API.ban_key, '{{"service_token": "{service_token}", "key": "{key}"}}'),
         'handle': apikeys_api.API.ban_key,
         'params': '{{"service_token": "{service_token}", "key": "{key}"}}',
         'expected': Expected(400, Error.api_key_already_banned)
         },

        # Test-case 25
        {'description': u'without service_token',
         'handle': apikeys_api.API.ban_key,
         'params': '{{"service_token": null, "key": "{key}"}}',
         'expected': Expected(400, Error.api_mandatory % ('ban_key', 'ban_key', 'service_token'))
         },

        # Test-case 26
        {'description': u'without key',
         'handle': apikeys_api.API.ban_key,
         'params': '{{"service_token": "{service_token}", "key": null}}',
         'expected': Expected(400, Error.api_mandatory % ('ban_key', 'ban_key', 'key'))
         },

        # project_service_link_export
        # Test-case 27
        {'description': u'invalid service_token',
         'handle': apikeys_api.API2.project_service_link_export,
         'params': '{{"service_token": "test"}}',
         'expected': Expected(403, Error.api_invalid_token_header)
         },

        # Test-case 28
        {'description': u'without service_token',
         'handle': apikeys_api.API2.project_service_link_export,
         'params': '{{"service_token": null}}',
         'expected': Expected(403, Error.api_invalid_token_header)
         },

        # Test-case 29
        {'description': u'invalid date',
         'handle': apikeys_api.API2.project_service_link_export,
         'params': '{{"service_token": "{service_token}", "update_dt": "test"}}',
         'expected': Expected(400, Error.api_invalid_date_format)
         },

        # Test-case 30
        {'description': u'invalid page_size',
         'handle': apikeys_api.API2.project_service_link_export,
         'params': '{{"service_token": "{service_token}", "page_size": ""}}',
         'expected': Expected(500, Error.api_500_invalid_optional_param)
         },

        # Test-case 31
        {'description': u'invalid page_number',
         'handle': apikeys_api.API2.project_service_link_export,
         'params': '{{"service_token": "{service_token}", "page_number": ""}}',
         'expected': Expected(500, Error.api_500_invalid_optional_param)
         }
    ],
    ids=lambda x: '{}-{}'.format(x.get('handle').func_name, x.get('description')))
@pytest.mark.good
def test_api(scenario, test_data):
    api_errors(scenario, test_data)


# ---------------------------------------------------------------------------------------------------------------------


@pytest.mark.parametrize(
    'scenario',
    [
        # get_user_info
        # Test-case 0
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.UI.get_user_info,
         'params': '{{"user_uid": "test"}}',
         'expected': Expected(400, Error.ui_invalid_oper_uid % ('get_user_info', 'get_user_info'))
         },

        # create_key
        # Test-case 1
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.UI.create_key,
         'params': '{{"oper_uid": "test"}}',
         'expected': Expected(400, Error.ui_invalid_oper_uid % ('create_key', 'create_key'))
         },

        # Test-case 2
        {'description': u'insufficient rights',
         'handle': apikeys_api.UI.create_key,
         'params': '{{"oper_uid": 0}}',
         'expected': Expected(403, Error.ui_forbidden)
         },

        # update_key
        # Test-case 3
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.UI.update_key,
         'params': '{{"oper_uid": "test", "key_id": "{key}", "name": "k1"}}',
         'expected': Expected(400, Error.ui_invalid_oper_uid % ('wrapper', 'wrapper'))
         },

        # Test-case 4
        {'description': u'invalid key_id',
         'handle': apikeys_api.UI.update_key,
         'params': '{{"oper_uid": {oper_uid}, "key_id": "test", "name": "k1"}}',
         'expected': Expected(404, Error.ui_key_id_not_found % 'test')
         },

        # Test-case 5
        {'description': u'without name',
         'handle': apikeys_api.UI.update_key,
         'params': '{{"oper_uid": {oper_uid}, "key_id": "{key}", "name": null}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'name'))
         },

        # Test-case 6
        {'description': u'without oper_uid',
         'handle': apikeys_api.UI.update_key,
         'params': '{{"oper_uid": null, "key_id": "{key}", "name": "k1"}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'oper_uid'))
         },

        # Test-case 7
        {'description': u'without key_id',
         'handle': apikeys_api.UI.update_key,
         'params': '{{"oper_uid": {oper_uid}, "key_id": null, "name": "k1"}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'key_id'))
         },

        # update_active
        # Test-case 8
        {'description': u'already active',
         # 'precondition': (apikeys_api.UI.update_active,
         #                  '{{"oper_uid": {oper_uid}, "key_id": "{key}", "service_id": {service_id}, "active": true}}'),
         'handle': apikeys_api.UI.update_active,
         'params': '{{"oper_uid": {oper_uid}, "key_id": "{key}", "service_id": {service_id}, "active": true}}',
         'expected': Expected(400, Error.ui_already_active)
         },

        # Test-case 9
        {'description': u'invalid service_id',
         'handle': apikeys_api.UI.update_active,
         'params': '{{"oper_uid": {oper_uid}, "key_id": "{key}", "service_id": 31, "active": true}}',
         'expected': Expected(404, Error.ui_invalid_service_id)
         },

        # Test-case 10
        {'description': u'invalid key_id',
         'handle': apikeys_api.UI.update_active,
         'params': '{{"oper_uid": {oper_uid}, "key_id": "test", "service_id": {service_id}, "active": true}}',
         'expected': Expected(404, Error.ui_key_id_not_found % 'test')
         },

        # Test-case 11
        {'description': u'without active',
         'handle': apikeys_api.UI.update_active,
         'params': '{{"oper_uid": {oper_uid}, "key_id": "{key}", "service_id": {service_id}, "active": null}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'active'))
         },

        # Test-case 12
        {'description': u'without key_id',
         'handle': apikeys_api.UI.update_active,
         'params': '{{"oper_uid": {oper_uid}, "key_id": null, "service_id": {service_id}, "active": true}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'key_id'))
         },

        # Test-case 13
        {'description': u'without oper_uid',
         'handle': apikeys_api.UI.update_active,
         'params': '{{"oper_uid": null, "key_id": "{key}", "service_id": {service_id}, "active": true}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'oper_uid'))
         },

        # update_service_link
        # Test-case 14
        {'description': u'invalid service_id',
         'handle': apikeys_api.UI.update_service_link,
         'params': '{{"oper_uid": {oper_uid}, "key_id": "{key}", "service_id": 777}}',
         'expected': Expected(404, Error.ui_service_id_not_found)
         },

        # Test-case 15
        {'description': u'invalid key_id',
         'handle': apikeys_api.UI.update_service_link,
         'params': '{{"oper_uid": {oper_uid}, "key_id": "test", "service_id": {service_id}}}',
         'expected': Expected(404, Error.ui_key_id_not_found % 'test')
         },

        # Test-case 16
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.UI.update_service_link,
         'params': '{{"oper_uid": "test", "key_id": "{key}", "service_id": {service_id}}}',
         'expected': Expected(400, Error.ui_invalid_oper_uid % ('wrapper', 'wrapper'))
         },

        # Test-case 17
        {'description': u'insufficient rights',
         'handle': apikeys_api.UI.update_service_link,
         'params': '{{"oper_uid": 0, "key_id": "{key}", "service_id": {service_id}}}',
         'expected': Expected(403, Error.ui_forbidden)
         },

        # Test-case 18
        {'description': u'without oper_uid',
         'handle': apikeys_api.UI.update_service_link,
         'params': '{{"oper_uid": null, "key_id": "{key}", "service_id": {service_id}}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'oper_uid'))
         },

        # Test-case 19
        {'description': u'without key_id',
         'handle': apikeys_api.UI.update_service_link,
         'params': '{{"oper_uid": {oper_uid}, "key_id": null, "service_id": {service_id}}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'key_id'))
         },

        # Test-case 20
        {'description': u'without service_id',
         'handle': apikeys_api.UI.update_service_link,
         'params': '{{"oper_uid": {oper_uid}, "key_id": "{key}", "service_id": null}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'service_id'))
         },

        # update_person
        # Test-case 21
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.UI.update_person,
         'params': '{{"oper_uid": "test", "project_id": "{project_id}", "service_id": {service_id},'
                   '"balance_person_id": "{person_id}"}}',
         'expected': Expected(400, Error.ui_invalid_oper_uid % ('wrapper', 'wrapper'))
         },

        # Test-case 22
        {'description': u'invalid project_id',
         'handle': apikeys_api.UI.update_person,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "test", "service_id": {service_id},'
                   '"balance_person_id": "{person_id}"}}',
         'expected': Expected(404, Error.ui_invalid_project_id % 'test')
         },

        # Test-case 23
        {'description': u'invalid service_id',
         'handle': apikeys_api.UI.update_person,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": 777,'
                   '"balance_person_id": "{person_id}"}}',
         'expected': Expected(404, Error.ui_service_id_not_in_project)
         },

        # Test-case 24
        {'description': u'invalid balance_person_id',
         'handle': apikeys_api.UI.update_person,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": {service_id},'
                   '"balance_person_id": "test"}}',
         'expected': Expected(404, Error.ui_person_not_found)
         },

        # Test-case 25
        {'description': u'without oper_uid',
         'handle': apikeys_api.UI.update_person,
         'params': '{{"oper_uid": null, "project_id": "{project_id}", "service_id": {service_id},'
                   '"balance_person_id": "{person_id}"}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'oper_uid'))
         },

        # Test-case 26
        {'description': u'without project_id',
         'handle': apikeys_api.UI.update_person,
         'params': '{{"oper_uid": {oper_uid}, "project_id": null, "service_id": {service_id},'
                   '"balance_person_id": "{person_id}"}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'project_id'))
         },

        # Test-case 27
        {'description': u'without service_id',
         'handle': apikeys_api.UI.update_person,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": null,'
                   '"balance_person_id": "{person_id}"}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'service_id'))
         },

        # Test-case 28
        {'description': u'without balance_person_id',
         'handle': apikeys_api.UI.update_person,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": {service_id},'
                   '"balance_person_id": null}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'balance_person_id'))
         },

        # list_project_links_financial
        # Test-case 29
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.UI.list_project_links_financial,
         'params': '{{"oper_uid": "test", "project_id": "{project_id}"}}',
         'expected': Expected(400, Error.ui_invalid_oper_uid % ('wrapper', 'wrapper'))
         },

        # Test-case 30
        {'description': u'invalid project_id',
         'handle': apikeys_api.UI.list_project_links_financial,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "test"}}',
         'expected': Expected(404, Error.ui_invalid_project_id % 'test')
         },

        # Test-case 31
        {'description': u'without oper_uid',
         'handle': apikeys_api.UI.list_project_links_financial,
         'params': '{{"oper_uid": null, "project_id": "{project_id}"}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'oper_uid'))
         },

        # Test-case 32
        {'description': u'without project_id',
         'handle': apikeys_api.UI.list_project_links_financial,
         'params': '{{"oper_uid": {oper_uid}, "project_id": null}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'project_id'))
         },

        # list_keys
        # Test-case 33
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.UI.list_keys,
         'params': '{{"oper_uid": "test"}}',
         'expected': Expected(400, Error.ui_invalid_oper_uid % ('list_keys', 'list_keys'))
         },

        # Test-case 34
        {'description': u'invalid page_size',
         'handle': apikeys_api.UI.list_keys,
         'params': '{{"oper_uid": {oper_uid}, "key_id": "test", "page_size": "test"}}',
         'expected': Expected(400, Error.ui_invalid_number_value % (('list_keys', 'page_size') * 2))
         },

        # Test-case 35
        {'description': u'invalid page',
         'handle': apikeys_api.UI.list_keys,
         'params': '{{"oper_uid": {oper_uid}, "key_id": "test", "page_size": 20,"page": "test"}}',
         'expected': Expected(400, Error.ui_invalid_number_value % (('list_keys', 'page') * 2))
         },

        # Test-case 36
        {'description': u'without oper_uid',
         'handle': apikeys_api.UI.list_keys,
         'params': '{{"oper_uid": null}}',
         'expected': Expected(400, Error.ui_mandatory % ('list_keys', 'list_keys', 'oper_uid'))
         },

        # key_blocks
        # Test-case 37
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.UI.key_blocks,
         'params': '{{"oper_uid": "test"}}',
         'expected': Expected(400, Error.ui_invalid_oper_uid % ('wrapper', 'wrapper'))
         },

        # Test-case 38
        {'description': u'invalid key_id',
         'handle': apikeys_api.UI.key_blocks,
         'params': '{{"oper_uid": {oper_uid}, "key_id": "test"}}',
         'expected': Expected(404, Error.ui_key_id_not_found % 'test')
         },

        # Test-case 39
        {'description': u'invalid page_size',
         'handle': apikeys_api.UI.key_blocks,
         'params': '{{"oper_uid": {oper_uid}, "key_id": "{key}", "page_size": "test"}}',
         'expected': Expected(400, Error.ui_invalid_number_value % (('wrapper', 'page_size') * 2))
         },

        # Test-case 40
        {'description': u'invalid page',
         'handle': apikeys_api.UI.key_blocks,
         'params': '{{"oper_uid": {oper_uid}, "key_id": "{key}", "page_size": 20,"page": "test"}}',
         'expected': Expected(400, Error.ui_invalid_number_value % (('wrapper', 'page') * 2))
         },

        # Test-case 41
        {'description': u'invalid from_dt',
         'handle': apikeys_api.UI.key_blocks,
         'params': '{{"oper_uid": {oper_uid}, "key_id": "{key}", "page_size": 20,"page": 1, "from_dt": "test"}}',
         'expected': Expected(400, Error.ui_invalid_date % (('wrapper', 'from_dt') * 2))
         },

        # Test-case 42
        {'description': u'invalid till_dt',
         'handle': apikeys_api.UI.key_blocks,
         'params': '{{"oper_uid": {oper_uid}, "key_id": "{key}", "till_dt": "test"}}',
         'expected': Expected(400, Error.ui_invalid_date % (('wrapper', 'till_dt') * 2))
         },

        # Test-case 43
        {'description': u'without oper_uid',
         'handle': apikeys_api.UI.key_blocks,
         'params': '{{"oper_uid": null}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'oper_uid'))
         },

        # Test-case 44
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.UI.key_usage_stat,
         'params': '{{"oper_uid": "test", "key_id": "{key}", "service_id": {service_id}}}',
         'expected': Expected(400, Error.ui_invalid_oper_uid % ('key_usage_stat', 'key_usage_stat'))
         },

        # Test-case 45
        {'description': u'invalid key_id',
         'handle': apikeys_api.UI.key_usage_stat,
         'params': '{{"oper_uid": {oper_uid}, "key_id": "test", "service_id": {service_id},"from_dt": "2017-06-13",'
                   ' "till_dt": "2017-07-13"}}',
         'expected': Expected(404, Error.ui_key_id_not_found % 'test')
         },

        # Test-case 46
        {'description': u'invalid service_id',
         'handle': apikeys_api.UI.key_usage_stat,
         'params': '{{"oper_uid": {oper_uid}, "key_id": "{key}", "service_id": "test","from_dt": "2017-06-13",'
                   ' "till_dt": "2017-07-13"}}',
         'expected': Expected(400, Error.ui_invalid_number_value % (('key_usage_stat', 'service_id') * 2))
         },

        # Test-case 47
        {'description': u'invalid from_dt',
         'handle': apikeys_api.UI.key_usage_stat,
         'params': '{{"oper_uid": {oper_uid}, "key_id": "{key}", "service_id": {service_id},"from_dt": "test",'
                   '"till_dt": "2017-07-13"}}',
         'expected': Expected(400, Error.ui_invalid_date % (('key_usage_stat', 'from_dt') * 2))
         },

        # Test-case 48
        {'description': u'invalid till_dt',
         'handle': apikeys_api.UI.key_usage_stat,
         'params': '{{"oper_uid": {oper_uid}, "key_id": "{key}", "service_id": {service_id},"from_dt": "2017-07-13",'
                   ' "till_dt": "test"}}',
         'expected': Expected(400, Error.ui_invalid_date % (('key_usage_stat', 'till_dt') * 2))
         },

        # Test-case 49
        {'description': u'without oper_uid',
         'handle': apikeys_api.UI.key_usage_stat,
         'params': '{{"oper_uid": null, "key_id": "{key}", "service_id": {service_id},"from_dt": "2017-07-12", '
                   '"till_dt": "2017-07-13"}}',
         'expected': Expected(400, Error.ui_mandatory % ('key_usage_stat', 'key_usage_stat', 'oper_uid'))
         },

        # Test-case 50
        {'description': u'without key_id',
         'handle': apikeys_api.UI.key_usage_stat,
         'params': '{{"oper_uid": {oper_uid}, "key_id": null, "service_id": {service_id}, "from_dt": "2017-07-12", '
                   '"till_dt": "2017-07-13"}}',
         'expected': Expected(400, Error.ui_mandatory_key_id_or_project_id)
         },

        # Test-case 51
        {'description': u'without service_id',
         'handle': apikeys_api.UI.key_usage_stat,
         'params': '{{"oper_uid": {oper_uid}, "key_id": "{key}", "service_id": null,"from_dt": "2017-07-12", '
                   '"till_dt": "2017-07-13"}}',
         'expected': Expected(400, Error.ui_mandatory % ('key_usage_stat', 'key_usage_stat', 'service_id'))
         },

        # get_request_deposit_personal_account
        # Test-case 52
        {'description': u'without person or contract',
         'handle': apikeys_api.UI.get_request_deposit_personal_account,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": {service_id},'
                   ' "amount": 5000}}',
         'expected': Expected(400, Error.ui_attach_person)
         },

        # Test-case 53
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.UI.get_request_deposit_personal_account,
         'params': '{{"oper_uid": "test", "project_id": "{project_id}", "service_id": {service_id}, "amount": 5000}}',
         'expected': Expected(400, Error.ui_invalid_oper_uid % (('wrapper',) * 2))
         },

        # Test-case 54
        {'description': u'invalid project_id',
         'handle': apikeys_api.UI.get_request_deposit_personal_account,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "test", "service_id": {service_id}, "amount": 50}}',
         'expected': Expected(404, Error.ui_invalid_project_id % 'test')
         },

        # Test-case 55
        {'description': u'invalid service_id',
         'handle': apikeys_api.UI.get_request_deposit_personal_account,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": 777, "amount": 5000}}',
         'expected': Expected(404, Error.ui_service_id_not_in_project)
         },

        # Test-case 56
        {'description': u'amount is zero',
         'precondition': (prepare_tariff_changing,
                          '{{"tariff": "{tariff}", "oper_uid": {oper_uid}, "project_id": "{project_id}", '
                          '"service_id": {service_id},"person_id": {person_id}, "key": "{key}"}}'),
         'handle': apikeys_api.UI.get_request_deposit_personal_account,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": {service_id}, "amount": 0}}',
         'expected': Expected(500, Error.ui_tariff_settings_is_broken)
         },

        # Test-case 57
        {'description': u'without oper_uid',
         'handle': apikeys_api.UI.get_request_deposit_personal_account,
         'params': '{{"oper_uid": null, "project_id": "{project_id}", "service_id": {service_id}, "amount": 10}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'oper_uid'))
         },

        # Test-case 58
        {'description': u'without project_id',
         'handle': apikeys_api.UI.get_request_deposit_personal_account,
         'params': '{{"oper_uid": {oper_uid}, "project_id": null, "service_id": {service_id}, "amount": 10}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'project_id'))
         },

        # Test-case 59
        {'description': u'without service_id',
         'handle': apikeys_api.UI.get_request_deposit_personal_account,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": null, "amount": 10}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'service_id'))
         },

        # Test-case 60
        {'description': u'without amount',
         'handle': apikeys_api.UI.get_request_deposit_personal_account,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": {service_id},'
                   '"amount": null}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'amount'))
         },

        # turn_on_tariff
        # Test-case 61
        {'description': u'without payment',
         'handle': apikeys_api.UI.turn_on_tariff,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": {service_id},'
                   '"tariff_cc": "{tariff}", "amount": 1}}',
         'expected': Expected(500, Error.ui_500_not_implemented)
         },

        # Test-case 62
        {'description': u'not enough',
         'precondition': (prepare_turn_on_tariff,
                          '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": {service_id},'
                          '"tariff_cc": "{tariff}", "person_id": {person_id},"key": "{key}", "client_id": '
                          '"{client_id}", "price": 1}}'),
         'handle': apikeys_api.UI.turn_on_tariff,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": {service_id},'
                   '"tariff_cc": "{tariff}", "amount": 1}}',
         'expected': Expected(500, Error.ui_not_enough)
         },

        # Test-case 63
        {'description': u'zero in amount',
         'precondition': (prepare_turn_on_tariff,
                          '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": {service_id},'
                          '"tariff_cc": "{tariff}", "person_id": {person_id},"key": "{key}",'
                          ' "client_id": "{client_id}", "price": {price}}}'),
         'handle': apikeys_api.UI.turn_on_tariff,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": {service_id},'
                   '"tariff_cc": "{tariff}", "amount": 0}}',
         'expected': Expected(500, Error.ui_500_turn_on)
         },

        # Test-case 64
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.UI.turn_on_tariff,
         'params': '{{"oper_uid": "test", "project_id": "{project_id}", "service_id": {service_id},'
                   '"tariff_cc": "{tariff}", "amount": 1}}',
         'expected': Expected(400, Error.ui_invalid_oper_uid % (('wrapper',) * 2))
         },

        # Test-case 65
        {'description': u'invalid project_id',
         'handle': apikeys_api.UI.turn_on_tariff,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "test", "service_id": {service_id},"tariff_cc": "{tariff}",'
                   ' "amount": 1}}',
         'expected': Expected(404, Error.ui_invalid_project_id % 'test')
         },

        # Test-case 66
        {'description': u'invalid service_id',
         'handle': apikeys_api.UI.turn_on_tariff,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": 777,"tariff_cc": "{tariff}",'
                   ' "amount": 1}}',
         'expected': Expected(404, Error.ui_service_id_not_in_project)
         },

        # Test-case 67
        {'description': u'invalid tariff_cc',
         'handle': apikeys_api.UI.turn_on_tariff,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": {service_id},'
                   '"tariff_cc": "test", "amount": 1}}',
         'expected': Expected(500, Error.ui_500_not_implemented)
         },

        # Test-case 68
        {'description': u'without oper_uid',
         'handle': apikeys_api.UI.turn_on_tariff,
         'params': '{{"oper_uid": null, "project_id": "{project_id}", "service_id": {service_id},'
                   '"tariff_cc": "{tariff}", "amount": 1}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'oper_uid'))
         },

        # Test-case 69
        {'description': u'without project_id',
         'handle': apikeys_api.UI.turn_on_tariff,
         'params': '{{"oper_uid": {oper_uid}, "project_id": null, "service_id": {service_id},"tariff_cc": "{tariff}",'
                   ' "amount": 1}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'project_id'))
         },

        # Test-case 70
        {'description': u'without service_id',
         'handle': apikeys_api.UI.turn_on_tariff,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": null,'
                   ' "tariff_cc": "{tariff}","amount": 1}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'service_id'))
         },

        # Test-case 71
        {'description': u'without tariff_cc',
         'handle': apikeys_api.UI.turn_on_tariff,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": {service_id},'
                   '"tariff_cc": null, "amount": 1}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'tariff_cc'))
         },

        # schedule_tariff_changing
        # Test-case 72
        {'description': u'invalid user_uid',
         'handle': apikeys_api.UI.schedule_tariff_changing,
         'params': '{{"oper_uid": {oper_uid}1, "project_id": "{project_id}", "service_id": {service_id},'
                   '"tariff": "{tariff}"}}',
         'expected': Expected(403, Error.ui_forbidden)
         },

        # Test-case 73
        {'description': u'invalid project_id',
         'handle': apikeys_api.UI.schedule_tariff_changing,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "test", "service_id": {service_id},"tariff": "{tariff}"}}',
         'expected': Expected(404, Error.ui_invalid_project_id % 'test')
         },

        # Test-case 74
        {'description': u'invalid service_id',
         'handle': apikeys_api.UI.schedule_tariff_changing,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": 777,"tariff": "{tariff}"}}',
         'expected': Expected(404, Error.ui_service_id_not_in_project)
         },

        # Test-case 75
        {'description': u'invalid tariff',
         'handle': apikeys_api.UI.schedule_tariff_changing,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": {service_id},'
                   '"tariff": "test"}}',
         'expected': Expected(404, Error.ui_tariff_not_found)
         },

        # Test-case 76
        {'description': u'not attachable tariff',
         'handle': apikeys_api.UI.schedule_tariff_changing,
         'params': '{{"oper_uid": {oper_uid_1}, "project_id": "{project_id_1}", "service_id": {service_id_1},'
                   '"tariff": "{tariff_1}"}}',
         'expected': Expected(400, Error.ui_tariff_is_not_attachable)
         },

        # Test-case 77
        {'description': u'without oper_uid',
         'handle': apikeys_api.UI.schedule_tariff_changing,
         'params': '{{"oper_uid": null, "project_id": "valid_1", "service_id": "valid_1","tariff": "{tariff}"}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'oper_uid'))
         },

        # cancel_tariff_changing
        # Test-case 78
        {'description': u'invalid user_uid',
         'handle': apikeys_api.UI.cancel_tariff_changing,
         'params': '{{"oper_uid": {oper_uid}1, "project_id": "{project_id}", "service_id": {service_id}}}',
         'expected': Expected(403, Error.ui_forbidden)
         },

        # Test-case 79
        {'description': u'invalid service_id',
         'handle': apikeys_api.UI.cancel_tariff_changing,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "{project_id}", "service_id": 777}}',
         'expected': Expected(404, Error.ui_service_id_not_in_project)
         },

        # Test-case 80
        {'description': u'invalid project_id',
         'handle': apikeys_api.UI.cancel_tariff_changing,
         'params': '{{"oper_uid": {oper_uid}, "project_id": "test", "service_id": {service_id}}}',
         'expected': Expected(404, Error.ui_invalid_project_id % 'test')
         },

        # Test-case 81
        {'description': u'without oper_uid',
         'handle': apikeys_api.UI.cancel_tariff_changing,
         'params': '{{"oper_uid": null, "project_id": "{project_id_1}", "service_id": {service_id_1}}}',
         'expected': Expected(400, Error.ui_mandatory % ('wrapper', 'wrapper', 'oper_uid'))
         },

        # Test-case 81
        {'description': u'not scheduled',
         'handle': apikeys_api.UI.cancel_tariff_changing,
         'params': '{{"oper_uid": {oper_uid_1}, "project_id": "{project_id_1}","service_id": {service_id_1}}}',
         'expected': Expected(400, Error.ui_tariff_is_not_scheduled)
         },

        # get_permissions
        # Test-case 82
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.UI.get_permissions,
         'params': '{{"oper_uid":"test"}}',
         'expected': Expected(400, Error.ui_invalid_oper_uid % ('get_permissions', 'get_permissions'))
         },

        # Test-case 83
        {'description': u'without oper_uid',
         'handle': apikeys_api.UI.get_permissions,
         'params': '{{"oper_uid":null}}',
         'expected': Expected(400, Error.ui_mandatory % ('get_permissions', 'get_permissions', 'oper_uid'))
         },

    ],
    ids=lambda x: '{}-{}'.format(x.get('handle').func_name, x.get('description')))
@pytest.mark.good
def test_ui(scenario, test_data):
    api_errors(scenario, test_data)


#
# # -------------------------------------------------------------------------------------------------------------------
#

@pytest.mark.parametrize(
    'scenario',
    [
        # get_audit_trail
        # Test-case 0
        {'description': u'insufficient rights',
         'handle': apikeys_api.BO.get_audit_trail,
         'params': '{{"oper_uid":{oper_uid}}}',
         'expected': Expected(403, Error.ui_forbidden)
         },

        # Test-case 1
        {'description': u'must be provided',
         'handle': apikeys_api.BO.get_audit_trail,
         'params': '{{"oper_uid":{admin}}}',
         'expected': Expected(400, Error.bo_must_be_provided % 'Login, Key or Uid')
         },

        # Test-case 2
        {'description': u'invalid login',
         'handle': apikeys_api.BO.get_audit_trail,
         'params': '{{"oper_uid":{admin}, "login":"nigol-dilavni-tset"}}',
         'expected': Expected(404, Error.bo_user_not_found)
         },

        # Test-case 3
        {'description': u'invalid uid',
         'handle': apikeys_api.BO.get_audit_trail,
         'params': '{{"oper_uid":{admin}, "uid":"test"}}',
         'expected': Expected(500, Error.bo_500_invalid_literal)
         },

        # Test-case 4
        {'description': u'invalid services',
         'handle': apikeys_api.BO.get_audit_trail,
         'params': '{{"oper_uid":{admin}, "login":"{login}", "services": "test"}}',
         'expected': Expected(400, Error.bo_bad_service_list)
         },

        # Test-case 5
        {'description': u'invalid page_size',
         'handle': apikeys_api.BO.get_audit_trail,
         'params': '{{"oper_uid":{admin}, "login":"{login}", "page_size": "test"}}',
         'expected': Expected(400, Error.ui_invalid_number_value % (('get_audit_trail', 'page_size') * 2))
         },

        # Test-case 6
        {'description': u'invalid page',
         'handle': apikeys_api.BO.get_audit_trail,
         'params': '{{"oper_uid":{admin}, "login":"{login}", "page": "test"}}',
         'expected': Expected(400, Error.ui_invalid_number_value % (('get_audit_trail', 'page') * 2))
         },

        # Test-case 7
        {'description': u'without oper_uid',
         'handle': apikeys_api.BO.get_audit_trail,
         'params': '{{ "oper_uid": null, "login":"{login}"}}',
         'expected': Expected(400, Error.ui_mandatory % ('get_audit_trail', 'get_audit_trail', 'oper_uid'))
         },

        # list_keys
        # Test-case 8
        {'description': u'insufficient rights',
         'handle': apikeys_api.BO.list_keys,
         'params': '{{"oper_uid":{oper_uid}}}',
         'expected': Expected(403, Error.ui_forbidden)
         },

        # Test-case 9
        {'description': u'invalid services',
         'handle': apikeys_api.BO.list_keys,
         'params': '{{"oper_uid":{admin}, "user_login":"{login}", "services": "test"}}',
         'expected': Expected(400, Error.bo_bad_service_list)
         },

        # Test-case 10
        {'description': u'without oper_uid',
         'handle': apikeys_api.BO.list_keys,
         'params': '{{"oper_uid":null, "user_login":"{login}"}}',
         'expected': Expected(400, Error.ui_mandatory % ('list_keys', 'list_keys', 'oper_uid'))
         },

        # get_permissions
        # Test-case 11
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.BO.get_permissions,
         'params': '{{"oper_uid":"test"}}',
         'expected': Expected(400, Error.ui_invalid_oper_uid % ('get_permissions', 'get_permissions'))
         },

        # Test-case 12
        {'description': u'without oper_uid',
         'handle': apikeys_api.BO.get_permissions,
         'params': '{{"oper_uid":null}}',
         'expected': Expected(400, Error.ui_mandatory % ('get_permissions', 'get_permissions', 'oper_uid'))
         },

        # get_user_info
        # # Test-case 13
        # {'description': u'must be provided',
        #  'handle': apikeys_api.BO.get_user_info,
        #  'params': '{{"oper_uid":{admin}, "user_uid":null, "user_login": null}}',
        #  'expected': Expected(400, Error.bo_must_be_provided % 'User_uid or user_login')
        #  },

        # # Test-case 14
        # {'description': u'insufficient rights',
        #  'handle': apikeys_api.BO.get_user_info,
        #  'params': '{{"oper_uid":{oper_uid}, "user_uid":null, "user_login": null}}',
        #  'expected': Expected(403, Error.ui_forbidden)
        #  },
        #
        # # Test-case 15
        # {'description': u'invalid oper_uid',
        #  'handle': apikeys_api.BO.get_user_info,
        #  'params': '{{"oper_uid":"test", "user_uid":null, "user_login": null}}',
        #  'expected': Expected(400, Error.ui_invalid_oper_uid % ('get_user_info', 'get_user_info'))
        #  },
        #
        # # Test-case 16
        # {'description': u'invalid user_uid',
        #  'handle': apikeys_api.BO.get_user_info,
        #  'params': '{{"oper_uid":{admin}, "user_uid":"test"}}',
        #  'expected': Expected(400, Error.bo_invalid_number_value % (('get_user_info', 'user_uid') * 2))
        #  },
        #
        # # Test-case 17
        # {'description': u'yandex passport not found',
        #  'handle': apikeys_api.BO.get_user_info,
        #  'params': '{{"oper_uid":{admin}, "user_uid":{oper_uid}, "user_login":0, "internal": true}}',
        #  'expected': Expected(404, Error.bo_yandex_passport_not_found)
        #  },
        #
        # # Test-case 18
        # {'description': u'without oper_uid',
        #  'handle': apikeys_api.BO.get_user_info,
        #  'params': '{{"oper_uid":null, "user_uid":null, "user_login":null}}',
        #  'expected': Expected(400, Error.ui_mandatory % ('get_user_info', 'get_user_info', 'oper_uid'))
        #  },

        # key_usage_stat
        # Test-case 19
        {'description': u'mandatory key id or project_id',
         'handle': apikeys_api.BO.key_usage_stat,
         'params': '{{"oper_uid":{admin}, "key_id":null, "project_id":null, "service_id":{service_id}}}',
         'expected': Expected(400, Error.ui_mandatory_key_id_or_project_id)
         },

        # Test-case 20
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.BO.key_usage_stat,
         'params': '{{"oper_uid":{oper_uid}, "key_id":"{key}", "project_id":"{project_id}",'
                   ' "service_id":{service_id}}}',
         'expected': Expected(403, Error.ui_forbidden)
         },

        # Test-case 21
        {'description': u'invalid from_dt',
         'handle': apikeys_api.BO.key_usage_stat,
         'params': '{{"oper_uid":{admin}, "key_id":"{key}", "project_id":"{project_id}", "service_id":{service_id}, '
                   '"from_dt":"test"}}',
         'expected': Expected(400, Error.ui_invalid_date % (('key_usage_stat', 'from_dt') * 2))
         },

        # Test-case 22
        {'description': u'invalid till_dt',
         'handle': apikeys_api.BO.key_usage_stat,
         'params': '{{"oper_uid":{admin}, "key_id":"{key}", "project_id":"{project_id}", "service_id":{service_id}, '
                   '"till_dt":"test"}}',
         'expected': Expected(400, Error.ui_invalid_date % (('key_usage_stat', 'till_dt') * 2))
         },

        # Test-case 23
        {'description': u'without oper_uid',
         'handle': apikeys_api.BO.key_usage_stat,
         'params': '{{"oper_uid":null, "key_id":"{key}", "project_id":"{project_id}", "service_id":{service_id}}}',
         'expected': Expected(400, Error.ui_mandatory % ('key_usage_stat', 'key_usage_stat', 'oper_uid'))
         },

        # activate_key
        # Test-case 24
        {'description': u'already approved',
         # 'precondition': (
         #         apikeys_api.BO.activate_key, '{{"oper_uid":{admin}, "key":"{key}", "service_id":{service_id}}}'),
         'handle': apikeys_api.BO.activate_key,
         'params': '{{"oper_uid":{admin}, "key":"{key}", "service_id":{service_id}}}',
         'expected': Expected(400, Error.bo_already_approved)
         },

        # Test-case 25
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.BO.activate_key,
         'params': '{{"oper_uid":{oper_uid}, "key":"{key}", "service_id":{service_id}}}',
         'expected': Expected(403, Error.ui_forbidden)
         },

        # Test-case 26
        {'description': u'invalid key',
         'handle': apikeys_api.BO.activate_key,
         'params': '{{"oper_uid":{admin}, "key":"test", "service_id":{service_id}}}',
         'expected': Expected(404, Error.bo_object_not_found % ('Key', '_id', "u'test'"))
         },

        # Test-case 27
        {'description': u'invalid service_id',
         'handle': apikeys_api.BO.activate_key,
         'params': '{{"oper_uid":{admin}, "key":"{key}", "service_id":777}}',
         'expected': Expected(404, Error.bo_object_not_found % ('Service', '_id', 777))
         },

        # Test-case 28
        {'description': u'without oper_uid',
         'handle': apikeys_api.BO.activate_key,
         'params': '{{"oper_uid":null, "key":"{key}", "service_id":{service_id}}}',
         'expected': Expected(400, Error.ui_mandatory % ('activate_key', 'activate_key', 'oper_uid'))
         },

        # Test-case 29
        {'description': u'without key',
         'handle': apikeys_api.BO.activate_key,
         'params': '{{"oper_uid":{admin}, "key":null, "service_id":{service_id}}}',
         'expected': Expected(400, Error.ui_mandatory % ('activate_key', 'activate_key', 'key'))
         },

        # Test-case 30
        {'description': u'without service_id',
         'handle': apikeys_api.BO.activate_key,
         'params': '{{"oper_uid":{admin}, "key":"{key}", "service_id":null}}',
         'expected': Expected(400, Error.ui_mandatory % ('activate_key', 'activate_key', 'service_id'))
         },

        # update_ban
        # Test-case 31
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.BO.update_ban,
         'params': '{{"oper_uid":{oper_uid}, "key":"{key}", "service_id":{service_id}, "ban":true, "reason_id":1}}',
         'expected': Expected(403, Error.ui_forbidden)
         },

        # Test-case 32
        {'description': u'invalid key',
         'handle': apikeys_api.BO.update_ban,
         'params': '{{"oper_uid":{admin}, "key":"test", "service_id":{service_id}, "ban":true, "reason_id":1}}',
         'expected': Expected(404, Error.bo_object_not_found % ('Key', '_id', "u'test'"))
         },

        # Test-case 33
        {'description': u'invalid service_id',
         'handle': apikeys_api.BO.update_ban,
         'params': '{{"oper_uid":{admin}, "key":"{key}", "service_id":777, "ban":true, "reason_id":1}}',
         'expected': Expected(404, Error.bo_object_not_found % ('Service', '_id', 777))
         },

        # Test-case 34
        {'description': u'invalid reason',
         'handle': apikeys_api.BO.update_ban,
         'params': '{{"oper_uid":{admin}, "key":"{key_1}", "service_id":{service_id_1}, "ban":true, "reason_id":10000}}',
         'expected': Expected(400, Error.bo_bad_reason)
         },

        # Test-case 35
        {'description': u'already banned',
         'precondition': (apikeys_api.BO.update_ban,
                          '{{"oper_uid":{admin}, "key":"{key_1}", "service_id":{service_id_1}, "ban":true,'
                          ' "reason_id":99}}'),
         'handle': apikeys_api.BO.update_ban,
         'params': '{{"oper_uid":{admin}, "key":"{key_1}", "service_id":{service_id_1}, "ban":true, "reason_id":99}}',
         'expected': Expected(400, Error.bo_already_banned)
         },

        # Test-case 36
        {'description': u'without oper_uid',
         'handle': apikeys_api.BO.update_ban,
         'params': '{{"oper_uid":null, "key":"{key}", "service_id":{service_id}, "ban":true, "reason_id":1}}',
         'expected': Expected(400, Error.ui_mandatory % ('update_ban', 'update_ban', 'oper_uid'))
         },

        # Test-case 37
        {'description': u'without key',
         'handle': apikeys_api.BO.update_ban,
         'params': '{{"oper_uid":{admin}, "key":null, "service_id":{service_id}, "ban":true, "reason_id":1}}',
         'expected': Expected(400, Error.ui_mandatory % ('update_ban', 'update_ban', 'key'))
         },

        # Test-case 38
        {'description': u'without ban',
         'handle': apikeys_api.BO.update_ban,
         'params': '{{"oper_uid":{admin}, "key":"{key}", "service_id":{service_id}, "ban":null, "reason_id":1}}',
         'expected': Expected(400, Error.ui_mandatory % ('update_ban', 'update_ban', 'ban'))
         },

        # Test-case 39
        {'description': u'without reason_id',
         'handle': apikeys_api.BO.update_ban,
         'params': '{{"oper_uid":{admin}, "key":"{key}", "service_id":{service_id}, "ban":true, "reason_id":null}}',
         'expected': Expected(400, Error.ui_mandatory % ('update_ban', 'update_ban', 'reason_id'))
         },

        # create_key
        # Test-case 40
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.BO.create_key,
         'params': '{{"oper_uid":{oper_uid}, "user_uid":{oper_uid}}}',
         'expected': Expected(403, Error.ui_forbidden)
         },

        # Test-case 41
        {'description': u'invalid user_uid',
         'handle': apikeys_api.BO.create_key,
         'params': '{{"oper_uid":{admin}, "user_uid":0}}',
         'expected': Expected(404, Error.bo_object_not_found % ('User', '_id', 0))
         },

        # Test-case 42
        {'description': u'without oper_uid',
         'handle': apikeys_api.BO.create_key,
         'params': '{{"oper_uid":null, "user_uid":{oper_uid}}}',
         'expected': Expected(400, Error.ui_mandatory % ('create_key', 'create_key', 'oper_uid'))
         },

        # Test-case 43
        {'description': u'without user_uid',
         'handle': apikeys_api.BO.create_key,
         'params': '{{"oper_uid":{admin}, "user_uid":null}}',
         'expected': Expected(400, Error.ui_mandatory % ('create_key', 'create_key', 'user_uid'))
         },

        # update_service_link
        # Test-case 45
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.BO.update_service_link,
         'params': '{{"oper_uid":{oper_uid}, "key":"{key}", "service_id":{service_id}}}',
         'expected': Expected(403, Error.ui_forbidden)
         },

        # Test-case 46
        {'description': u'invalid key',
         'handle': apikeys_api.BO.update_service_link,
         'params': '{{"oper_uid":{admin}, "key":"test", "service_id":{service_id}}}',
         'expected': Expected(404, Error.bo_object_not_found % ('Key', '_id', "u'test'"))
         },

        # Test-case 47
        {'description': u'invalid service_id',
         'handle': apikeys_api.BO.update_service_link,
         'params': '{{"oper_uid":{admin}, "key":"{key}", "service_id":777}}',
         'expected': Expected(404, Error.bo_object_not_found % ('Service', '_id', 777))
         },

        # Test-case 48
        {'description': u'without oper_uid',
         'handle': apikeys_api.BO.update_service_link,
         'params': '{{"oper_uid":null, "key":"{key}", "service_id":{service_id}}}',
         'expected': Expected(400, Error.ui_mandatory % ('update_service_link', 'update_service_link', 'oper_uid'))
         },

        # Test-case 49
        {'description': u'without key',
         'handle': apikeys_api.BO.update_service_link,
         'params': '{{"oper_uid":{admin}, "key":null, "service_id":{service_id}}}',
         'expected': Expected(400, Error.bo_key_id_is_mandatory)
         },

        # Test-case 50
        {'description': u'without service_id',
         'handle': apikeys_api.BO.update_service_link,
         'params': '{{"oper_uid":{admin}, "key":"{key}", "service_id":null}}',
         'expected': Expected(400, Error.ui_mandatory % ('update_service_link', 'update_service_link', 'service_id'))
         },

        # get_link_info
        # Test-case 51
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.BO.get_link_info,
         'params': '{{"oper_uid":{oper_uid}, "key":"{key}", "service_id":{service_id}}}',
         'expected': Expected(403, Error.ui_forbidden)
         },

        # Test-case 52
        {'description': u'invalid key',
         'handle': apikeys_api.BO.get_link_info,
         'params': '{{"oper_uid":{admin}, "key":"test", "service_id":{service_id}}}',
         'expected': Expected(404, Error.bo_link_not_found)
         },

        # Test-case 53
        {'description': u'invalid service_id',
         'handle': apikeys_api.BO.get_link_info,
         'params': '{{"oper_uid":{admin}, "key":"{key}", "service_id":777}}',
         'expected': Expected(404, Error.bo_link_not_found)
         },

        # Test-case 54
        {'description': u'without oper_uid',
         'handle': apikeys_api.BO.get_link_info,
         'params': '{{"oper_uid":null, "key":"{key}", "service_id":{service_id}}}',
         'expected': Expected(400, Error.ui_mandatory % ('get_link_info', 'get_link_info', 'oper_uid'))
         },

        # Test-case 55
        {'description': u'without key',
         'handle': apikeys_api.BO.get_link_info,
         'params': '{{"oper_uid":{admin}, "key":null, "service_id":{service_id}}}',
         'expected': Expected(400, Error.ui_mandatory % ('get_link_info', 'get_link_info', 'key'))
         },

        # push_tariffs_to_ballance
        # Test-case 56
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.BO.push_tariffs_to_ballance,
         'params': '{{"oper_uid":{oper_uid}}}',
         'expected': Expected(403, Error.ui_forbidden)
         },

        # Test-case 57
        {'description': u'without oper_uid',
         'handle': apikeys_api.BO.push_tariffs_to_ballance,
         'params': '{{"oper_uid":null}}',
         'expected': Expected(400, Error.ui_mandatory % ('push_tariffs_to_ballance', 'push_tariffs_to_ballance',
                                                         'oper_uid'))
         },

        # get_tariff_tree
        # Test-case 58
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.BO.get_tariff_tree,
         'params': '{{"oper_uid":{oper_uid}}}',
         'expected': Expected(403, Error.ui_forbidden)
         },

        # Test-case 59
        {'description': u'without oper_uid',
         'handle': apikeys_api.BO.get_tariff_tree,
         'params': '{{"oper_uid":null}}',
         'expected': Expected(400, Error.ui_mandatory % ('get_tariff_tree', 'get_tariff_tree', 'oper_uid'))
         },

        # update_unblockable
        # Test-case 60
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.BO.update_unblockable,
         'params': '{{"oper_uid":{oper_uid}, "key":"{key}", "service_id":{service_id}, "unblockable":true}}',
         'expected': Expected(403, Error.bo_forbidden)
         },

        # Test-case 61
        {'description': u'invalid key',
         'handle': apikeys_api.BO.update_unblockable,
         'params': '{{"oper_uid":{admin}, "key":"test", "service_id":{service_id}, "unblockable":true}}',
         'expected': Expected(404, Error.bo_invalid_key_id % "test")
         },

        # Test-case 62
        {'description': u'invalid service_id',
         'handle': apikeys_api.BO.update_unblockable,
         'params': '{{"oper_uid":{admin}, "key":"{key}", "service_id":777, "unblockable":true}}',
         'expected': Expected(404, Error.bo_invalid_service_id)
         },

        # Test-case 63
        {'description': u'invalid unblockable',
         'handle': apikeys_api.BO.update_unblockable,
         'params': '{{"oper_uid":{admin}, "key":"{key}", "service_id":{service_id}, "unblockable":0}}',
         'expected': Expected(400, Error.bo_already_applied)
         },

        # Test-case 64
        {'description': u'without oper_uid',
         'handle': apikeys_api.BO.update_unblockable,
         'params': '{{"oper_uid":null, "key":"{key}", "service_id":{service_id}, "unblockable":0}}',
         'expected': Expected(400, Error.bo_oper_uid_is_mandatory)
         },

        # Test-case 65
        {'description': u'without key',
         'handle': apikeys_api.BO.update_unblockable,
         'params': '{{"oper_uid":{admin}, "key":null, "service_id":{service_id}, "unblockable":0}}',
         'expected': Expected(400, Error.bo_key_id_is_mandatory_unblockable)
         },

        # Test-case 66
        {'description': u'without service_id',
         'handle': apikeys_api.BO.update_unblockable,
         'params': '{{"oper_uid":{admin}, "key":"{key}", "service_id":null, "unblockable":0}}',
         'expected': Expected(400, Error.ui_mandatory % ('update_unblockable', 'update_unblockable', 'service_id'))
         },

        # Test-case 67
        {'description': u'without unblockable',
         'handle': apikeys_api.BO.update_unblockable,
         'params': '{{"oper_uid":{admin}, "key":"{key}", "service_id":{service_id}, "unblockable":null}}',
         'expected': Expected(400, Error.bo_unblockable_is_mandatory)
         },

        # get_client_from_balance
        # Test-case 68
        {'description': u'invalid oper_uid',
         'handle': apikeys_api.BO.get_client_from_balance,
         'params': '{{"oper_uid":{oper_uid}, "user_uid":{oper_uid}}}',
         'expected': Expected(403, Error.ui_forbidden)
         },

        # Test-case 69
        {'description': u'invalid user_uid',
         'handle': apikeys_api.BO.get_client_from_balance,
         'params': '{{"oper_uid":{admin}, "user_uid":0}}',
         'expected': Expected(404, Error.bo_object_not_found % ('User', '_id', 0))
         },

        # Test-case 70
        {'description': u'without oper_uid',
         'handle': apikeys_api.BO.get_client_from_balance,
         'params': '{{"oper_uid":null, "user_uid":{oper_uid}}}',
         'expected': Expected(400, Error.ui_mandatory % ('get_client_from_balance', 'get_client_from_balance',
                                                         'oper_uid'))
         },

        # Test-case 71
        {'description': u'without user_uid',
         'handle': apikeys_api.BO.get_client_from_balance,
         'params': '{{"oper_uid":{admin}, "user_uid":null}}',
         'expected': Expected(400, Error.ui_mandatory % ('get_client_from_balance', 'get_client_from_balance',
                                                         'user_uid'))
         },

        # schedule_tariff_changing
        # Test-case 72
        {'description': u'invalid user_uid',
         'handle': apikeys_api.BO.schedule_tariff_changing,
         'params': '{{"oper_uid":{oper_uid}, "key":"{key}", "service_id":{service_id},"tariff":"{tariff}"}}',
         'expected': Expected(403, Error.ui_forbidden)
         },

        # Test-case 73
        {'description': u'invalid key',
         'handle': apikeys_api.BO.schedule_tariff_changing,
         'params': '{{"oper_uid":{admin}, "key":"test", "service_id":{service_id},"tariff":"{tariff}"}}',
         'expected': Expected(404, Error.bo_link_not_found)
         },

        # Test-case 74
        {'description': u'invalid service_id',
         'handle': apikeys_api.BO.schedule_tariff_changing,
         'params': '{{"oper_uid":{admin}, "key":"{key}", "service_id":777, "tariff":"{tariff}"}}',
         'expected': Expected(404, Error.bo_link_not_found)
         },

        # Test-case 75
        {'description': u'not attachable tariff',
         'handle': apikeys_api.BO.schedule_tariff_changing,
         'params': '{{"oper_uid":{admin}, "key":"{key}", "service_id":{service_id},'
                   '"tariff":"speechkitcloud_client_201702"}}',
         'expected': Expected(400, Error.bo_not_attachable)
         },

        # Test-case 76
        {'description': u'invalid tariff',
         'handle': apikeys_api.BO.schedule_tariff_changing,
         'params': '{{"oper_uid":{admin}, "key":"{key}", "service_id":{service_id},"tariff":"test"}}',
         'expected': Expected(404, Error.bo_object_not_found % ('Tariff', 'cc', "u'test'"))
         },

        # Test-case 77
        {'description': u'without oper_uid',
         'handle': apikeys_api.BO.schedule_tariff_changing,
         'params': '{{"oper_uid":null, "key":"{key}", "service_id":{service_id},"tariff":"test"}}',
         'expected': Expected(400, Error.ui_mandatory % ('schedule_tariff_changing', 'schedule_tariff_changing',
                                                         'oper_uid'))
         },

        # Test-case 78
        {'description': u'without key',
         'handle': apikeys_api.BO.schedule_tariff_changing,
         'params': '{{"oper_uid":{admin}, "key":null, "service_id":{service_id},"tariff":"test"}}',
         'expected': Expected(400, Error.ui_mandatory % ('schedule_tariff_changing', 'schedule_tariff_changing',
                                                         'key'))
         },

        # Test-case 79
        {'description': u'without service_id',
         'handle': apikeys_api.BO.schedule_tariff_changing,
         'params': '{{"oper_uid":{admin}, "key":"{key}", "service_id":null,"tariff":"test"}}',
         'expected': Expected(400, Error.ui_mandatory % ('schedule_tariff_changing', 'schedule_tariff_changing',
                                                         'service_id'))
         },

        # Test-case 80
        {'description': u'without tariff',
         'handle': apikeys_api.BO.schedule_tariff_changing,
         'params': '{{"oper_uid":{admin}, "key":"{key}", "service_id":{service_id},"tariff":null}}',
         'expected': Expected(400, Error.ui_mandatory % ('schedule_tariff_changing', 'schedule_tariff_changing',
                                                         'tariff'))
         },

    ],
    ids=lambda x: '{}-{}'.format(x.get('handle').func_name, x.get('description')))
@pytest.mark.good
def test_bo(scenario, test_data):
    api_errors(scenario, test_data)
