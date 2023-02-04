# coding: utf-8

import json
import time
from copy import deepcopy
from datetime import timedelta as shift
from decimal import Decimal as D

from hamcrest import greater_than, is_in, equal_to, is_, not_, greater_than_or_equal_to

from apikeys.apikeys_utils import Verifier
from apikeys.tests_by_typical_flows import plain_function as plain
from apikeys import apikeys_steps, apikeys_api, apikeys_utils
from balance import balance_steps as steps, balance_db as db, balance_api as api
from apikeys.apikeys_defaults import BASE_DT, ADMIN, WAITER_PARAMS as W
from btestlib import utils
from btestlib.utils import wait_until2 as wait
from btestlib import matchers as mtch

__author__ = 'kostya-karpus'

START_PREVIOUS_MONTH, END_PREVIOUS_MONTH = utils.Date.previous_month_first_and_last_days(BASE_DT)
START_CURRENT_MONTH, END_CURRENT_MONTH = utils.Date.current_month_first_and_last_days(BASE_DT)


class Contract():
    @staticmethod
    def general_contract(scenario, db_connection, service_cc):
        oper_uid, login, token, service_id, key, client_id, person_id = apikeys_steps.prepare_key(db_connection,
                                                                                                  service_cc)

        apikeys_steps.create_contract(scenario, service_cc, client_id, person_id, scenario.dates)

        apikeys_api.BO().get_client_from_balance(ADMIN, oper_uid)
        apikeys_api.TEST().run_user_contractor(oper_uid)
        link_id = apikeys_steps.get_link_by_key(key)[0]['link_id']
        apikeys_api.TEST.run_tarifficator(link_id)

        if scenario.contract_activated:
            wait(apikeys_steps.get_link_tariff, mtch.equal_to(scenario.tariff.replace('apikeys_', '')), timeout=W.time,
                 sleep_time=W.s_time)(key, oper_uid, token)
        else:
            wait(apikeys_steps.get_link_tariff, not_(mtch.equal_to(scenario.tariff.replace('apikeys_', ''))),
                 timeout=W.time, sleep_time=W.s_time)(key, oper_uid, token)

    @staticmethod
    def stopped_contract(scenario, db_connection, service_cc):
        oper_uid, login, token, service_id, key, client_id, person_id = apikeys_steps.prepare_key(db_connection,
                                                                                                  service_cc)

        params = {'CLIENT_ID': client_id,
                  'PERSON_ID': person_id,
                  'DT': scenario.dates.get('DT').strftime('%Y-%m-%dT00:00:00'),
                  'FINISH_DT': scenario.dates.get('FINISH_DT').strftime('%Y-%m-%dT00:00:00'),
                  'IS_SIGNED': scenario.dates.get('IS_SIGNED').strftime('%Y-%m-%dT00:00:00'),
                  'APIKEYS_TARIFFS': apikeys_steps.get_apikeys_tariffs(scenario, service_cc),
                  }

        contract_id, _ = steps.ContractSteps.create_contract('no_agency_apikeys_post', params)

        apikeys_api.BO().get_client_from_balance(ADMIN, oper_uid)

        utils.check_that(apikeys_steps.get_link_tariff(key, oper_uid, token),
                         mtch.equal_to(scenario.tariff.replace('apikeys_', '')),
                         u'Проверяем наличие тарифа из договора в связке')

        params.update({'ID': contract_id,
                       scenario.stop_type: (BASE_DT - shift(days=1)).strftime('%Y-%m-%dT00:10:00')})

        steps.ContractSteps.create_contract('no_agency_apikeys_post', params)

        utils.check_that(apikeys_steps.get_link_tariff(key, oper_uid, token),
                         not_(mtch.equal_to(scenario.tariff.replace('apikeys_', ''))),
                         u'Проверяем отсутствие тарифа из договора в связке')

    @staticmethod
    def collateral_contract(scenario, db_connection, service_cc):
        oper_uid, login, token, service_id, key, client_id, person_id = apikeys_steps.prepare_key(db_connection,
                                                                                                  service_cc)

        contract_id = apikeys_steps.create_contract(scenario, service_cc, client_id, person_id, scenario.dates)

        apikeys_api.BO().get_client_from_balance(ADMIN, oper_uid)
        apikeys_api.TEST.run_user_contractor(oper_uid)

        utils.check_that(apikeys_steps.get_link_tariff(key, oper_uid, token),
                         mtch.equal_to(scenario.tariff.replace('apikeys_', '')),
                         u'Проверяем наличие тарифа из договора в связке')

        if scenario.collateral_contract.get('tariff'):
            # change tariff in contract
            new_tariff = scenario.collateral_contract['tariff']
            steps.ContractSteps.create_collateral(scenario.collateral_contract.get('_type'),
                                                  {'CONTRACT2_ID': contract_id,
                                                   'DT': scenario.dates.get('IS_FAXED').strftime('%Y-%m-%dT00:00:00'),
                                                   'IS_FAXED': scenario.dates.get('IS_FAXED').strftime(
                                                       '%Y-%m-%dT00:00:00'),
                                                   'IS_BOOKED': scenario.dates.get('IS_FAXED').strftime(
                                                       '%Y-%m-%dT00:00:00'),
                                                   'FINISH_DT': scenario.dates.get('FINISH_DT').strftime(
                                                       '%Y-%m-%dT00:00:00'),
                                                   'APIKEYS_TARIFFS': apikeys_steps.get_apikeys_tariffs(
                                                       utils.aDict(scenario.collateral_contract),
                                                       service_cc)
                                                   })

            apikeys_api.BO().get_client_from_balance(ADMIN, oper_uid)

            utils.check_that(apikeys_steps.get_link_tariff(key, oper_uid, token),
                             mtch.equal_to(scenario.tariff.replace('apikeys_', '')),
                             u'Проверяем наличие тарифа из договора в связке')

        else:
            # terminate contract
            steps.ContractSteps.create_collateral(scenario.collateral_contract.get('_type'),
                                                  {'CONTRACT2_ID': contract_id,
                                                   'DT': scenario.dates.get('DT').strftime('%Y-%m-%dT00:00:00'),
                                                   'IS_FAXED': scenario.dates.get('IS_FAXED').strftime(
                                                       '%Y-%m-%dT00:00:00'),
                                                   'IS_BOOKED': scenario.dates.get('IS_FAXED').strftime(
                                                       '%Y-%m-%dT00:00:00'),
                                                   'FINISH_DT': scenario.dates.get('IS_FAXED').strftime(
                                                       '%Y-%m-%dT00:00:00'),

                                                   })
            apikeys_api.TEST.run_user_contractor(oper_uid)
            link_id = apikeys_steps.get_link_by_key(key)[0]['link_id']
            apikeys_api.TEST.run_tarifficator(link_id)

            utils.check_that(apikeys_steps.get_link_tariff(key, oper_uid, token),
                             not_(mtch.equal_to(scenario.tariff.replace('apikeys_', ''))),
                             u'Проверяем отсутствие тарифа из договора в связке')

    @staticmethod
    def change_contract(scenario, db_connection, service_cc):
        oper_uid, login, token, service_id, key, client_id, person_id = apikeys_steps.prepare_key(db_connection,
                                                                                                  service_cc)

        params = {'CLIENT_ID': client_id,
                  'PERSON_ID': person_id,
                  'APIKEYS_TARIFFS': apikeys_steps.get_apikeys_tariffs(scenario, service_cc),
                  }
        params_old1 = deepcopy(params)
        params_old1.update({key: value.strftime('%Y-%m-%dT00:00:00') for key, value in scenario.dates_old1.items()})

        contract_id, _ = steps.ContractSteps.create_contract('no_agency_apikeys_post', params_old1)

        apikeys_api.BO().get_client_from_balance(ADMIN, oper_uid)

        utils.check_that(apikeys_steps.get_link_tariff(key, oper_uid, token),
                         mtch.equal_to(scenario.tariff.replace('apikeys_', '')),
                         u'Проверяем наличие тарифа из договора в связке')

        dates_old2 = deepcopy(params)
        dates_old2.update({'ID': contract_id})
        dates_old2.update({key: value.strftime('%Y-%m-%dT00:00:00') for key, value in scenario.dates_old2.items()})
        steps.ContractSteps.create_contract('no_agency_apikeys_post', dates_old2)
        link_id = apikeys_steps.get_link_by_key(key)[0]['link_id']
        apikeys_api.TEST.run_user_contractor(oper_uid)
        apikeys_api.TEST.run_tarifficator(link_id)

        utils.check_that(apikeys_steps.get_link_tariff(key, oper_uid, token),
                         not_(mtch.equal_to(scenario.tariff.replace('apikeys_', ''))),
                         u'Проверяем отсутствие тарифа из договора в связке')

        params_new = deepcopy(params)
        params_new.update({key: value.strftime('%Y-%m-%dT00:00:00') for key, value in scenario.dates_new.items()})
        steps.ContractSteps.create_contract('no_agency_apikeys_post', params_new)

        apikeys_api.TEST.run_user_contractor(oper_uid)
        apikeys_api.TEST.run_tarifficator(link_id)
        utils.check_that(apikeys_steps.get_link_tariff(key, oper_uid, token),
                         mtch.equal_to(scenario.tariff.replace('apikeys_', '')),
                         u'Проверяем наличие тарифа из договора в связке')


class LimitChecker():
    @staticmethod
    def free_usage(scenario, free_passport, service_cc, expected_banned):
        oper_uid, login, token, service_id, key, client_id, person_id = apikeys_steps.prepare_key(free_passport,
                                                                                                  service_cc)

        apikeys_steps.approve_key(key)
        apikeys_api.TEST().run_user_contractor(oper_uid)
        link_id = apikeys_steps.get_link_by_key(key)[0]['link_id']

        if hasattr(scenario, 'processed') and not hasattr(scenario, 'raw'):
            counters = apikeys_steps.counter_initialization(scenario.processed, token, key)
            apikeys_steps.process_all_stats(utils.aDict(scenario.processed), counters, oper_uid)
        elif not hasattr(scenario, 'processed') and hasattr(scenario, 'raw'):
            apikeys_steps.counter_initialization(utils.aDict(scenario.raw), token, key)
            apikeys_steps.process_stats_raw(utils.aDict(scenario.raw), token, key)
        elif hasattr(scenario, 'processed') and hasattr(scenario, 'raw'):
            counters = apikeys_steps.counter_initialization(utils.aDict(scenario.processed), token, key)
            apikeys_steps.process_mixed_stats(scenario, counters, oper_uid, token, key, link_id)
        else:
            raise ValueError(
                "Invalid scenario format. " +
                "The scenario must have processed or raw section or both. Given scenario: {}".format(
                    str(scenario)))

        project_id = apikeys_steps.get_project_id_by_key(key)
        link_id = apikeys_api.TEST().mongo_find('project_service_link', {"project_id": project_id})[0]['_id']
        result = apikeys_api.TEST().run_limit_checker(link_id=link_id).json()
        # limit_checker results asserts
        utils.check_that(len(result["limit_checker"]), greater_than(0),
                         u'Выполняем проверку наличия limit_checker')
        utils.check_that(result["result"], (mtch.equal_to(not expected_banned)),
                         u'Выполняем проверку статуса limit_checker')
        utils.check_that(apikeys_api.API.get_link_info(token, key).json()['active'],
                         (mtch.equal_to(not expected_banned)),
                         u'Выполняем проверку связки')

        key_service_config = apikeys_api.TEST().mongo_find('project_service_link', {"project_id": project_id})
        utils.check_that(key_service_config[0]["config"]["banned"], (mtch.equal_to(expected_banned)),
                         u'Выполняем проверку статуса banned')
        utils.check_that(key_service_config[0]['config']['approved'], (mtch.equal_to(True)),
                         u'Выполняем проверку статуса approved')
        if key_service_config[0]["config"]["banned"]:
            limit_config_list = apikeys_steps.get_limit_configs_by_service_id(key_service_config[0]['service_id'])
            utils.check_that(long(key_service_config[0]['config']['ban_reason_id']),
                             is_in([long(limit['lock_reason']) for limit in limit_config_list]),
                             u'Выполняем проверку ban_reason_id')

        orders = db.get_order_by_client(client_id)
        assert len(orders) == 0  # check if we have no orders

    @staticmethod
    def multikeys_free_usage(scenario, free_passport, service_cc, expected_banned):
        keys_count = len(scenario)
        oper_uid, login, token, service_id, keys, client_id, person_id = apikeys_steps.prepare_several_keys(
            free_passport,
            service_cc,
            keys_count,
            keys_count)
        link_id = apikeys_steps.get_link_by_key(keys.linked[0])[0]['link_id']

        for key in keys.linked:
            apikeys_steps.approve_key(key)

        key_stats = dict(zip(keys.linked, scenario))

        for key, value in key_stats.items():
            key_scenario = utils.aDict(value)
            if hasattr(key_scenario, 'processed') and not hasattr(key_scenario, 'raw'):
                counters = apikeys_steps.counter_initialization(key_scenario.processed, token, key)
                apikeys_steps.process_all_stats(utils.aDict(key_scenario.processed), counters, oper_uid)
            elif not hasattr(key_scenario, 'processed') and hasattr(key_scenario, 'raw'):
                apikeys_steps.counter_initialization(utils.aDict(key_scenario.raw), token, key)
                apikeys_steps.process_stats_raw(utils.aDict(key_scenario.raw), token, key)
            elif hasattr(key_scenario, 'processed') and hasattr(key_scenario, 'raw'):
                counters = apikeys_steps.counter_initialization(utils.aDict(key_scenario.processed), token, key)
                apikeys_steps.process_mixed_stats(key_scenario, counters, oper_uid, token, key, link_id)
            else:
                raise ValueError(
                    "Invalid scenario format. " +
                    "The scenario must have processed or raw section or both. Given scenario: {}".format(
                        str(scenario)))

        project_id = apikeys_steps.get_project_id_by_key(keys.linked[0])
        link_id = apikeys_api.TEST().mongo_find('project_service_link', {"project_id": project_id})[0]['_id']
        result = apikeys_api.TEST().run_limit_checker(link_id=link_id).json()
        # limit_checker results asserts
        utils.check_that(len(result["limit_checker"]), greater_than(0),
                         u'Выполняем проверку наличия limit_checker')
        utils.check_that(result["result"], (mtch.equal_to(not expected_banned)),
                         u'Выполняем проверку статуса limit_checker')
        utils.check_that(apikeys_api.API.get_link_info(token, key).json()['active'],
                         (mtch.equal_to(not expected_banned)),
                         u'Выполняем проверку связки')

        key_service_config = apikeys_api.TEST().mongo_find('project_service_link', {"project_id": project_id})
        utils.check_that(key_service_config[0]["config"]["banned"], (mtch.equal_to(expected_banned)),
                         u'Выполняем проверку статуса banned')
        utils.check_that(key_service_config[0]['config']['approved'], (mtch.equal_to(True)),
                         u'Выполняем проверку статуса approved')
        if key_service_config[0]["config"]["banned"]:
            limit_config_list = apikeys_steps.get_limit_configs_by_service_id(key_service_config[0]['service_id'])
            utils.check_that(long(key_service_config[0]['config']['ban_reason_id']),
                             is_in([long(limit['lock_reason']) for limit in limit_config_list]),
                             u'Выполняем проверку ban_reason_id')

        orders = db.get_order_by_client(client_id)
        assert len(orders) == 0  # check if we have no orders

    @staticmethod
    def paid(scenario, free_passport, service_cc, expected_banned):
        oper_uid, login, token, service_id, key, client_id, person_id = apikeys_steps.prepare_key(free_passport,
                                                                                                  service_cc)

        contract_id = apikeys_steps.create_future_postpay_contract(scenario, service_cc, client_id, person_id)

        # apikeys_api.BO().set_link_paid(ADMIN, key, service_id)
        apikeys_api.BO().get_client_from_balance(ADMIN, oper_uid)
        apikeys_api.TEST().run_user_contractor(oper_uid)  # Store all contract; no tarifficator_state on this step.
        # Tariff will be specified on previous step.
        apikeys_api.BO().invoice_for_future(ADMIN, key, service_id)

        prepayment_invoice_id = db.get_invoices_by_contract_id(contract_id)[0]['id']
        print '[DEBUG] ...... Prepayment_invoice_id: {}'.format(prepayment_invoice_id)
        steps.InvoiceSteps.pay(prepayment_invoice_id)
        # check for notification (sent from Billing)
        orders = db.get_order_by_client(client_id)
        steps.CommonSteps.wait_and_get_notification(1, orders[0]['id'], 1)

        balance_tid = api.test_balance().GetNotification(1, orders[0]['id'])[0]['args'][0]['Tid']
        apikeys_tid = \
            apikeys_api.TEST.mongo_find('balance_order',
                                        {'project_id': {'$eq': apikeys_steps.get_projects(oper_uid)['_id']}})[
                0]['last_balance_notify_order_2_tid']
        assert long(balance_tid) == long(apikeys_tid)

        project = apikeys_steps.get_projects(oper_uid)
        project_id = project['_id']
        print '[DEBUG] ...... Project: {}'.format(project_id)

        key_approved = apikeys_api.TEST().mongo_find('project_service_link', {"project_id": project_id})[0]['config'][
            'approved']
        assert key_approved == False

        # Move contract start_Dt
        db.oracle_update('t_contract_collateral',
                         {'dt': "TO_DATE('{0}', 'YYYY-MM-DD HH24:MI:SS')".format(
                             BASE_DT.replace(microsecond=0) - shift(days=60))},
                         {'contract2_id': contract_id, 'num': None})

        # Move invoice dt
        db.oracle_update('t_invoice',
                         {'dt': "TO_DATE('{0}', 'YYYY-MM-DD HH24:MI:SS')".format(
                             BASE_DT.replace(microsecond=0))},
                         {'id': prepayment_invoice_id})

        apikeys_api.TEST().run_user_contractor(oper_uid)
        key_approved = apikeys_api.TEST().mongo_find('project_service_link', {"project_id": project_id})[0]['config'][
            'approved']
        assert key_approved == True
        actual_tariff = 'apikeys_{}'.format(
            apikeys_api.TEST.mongo_find('balance_order', {'project_id': {'$eq': project_id}})[0]['tariff'])
        assert actual_tariff == scenario['tariff']

        link_id = apikeys_steps.get_link_by_key(key)[0]['link_id']

        if 'processed' in scenario and 'raw' not in scenario:
            counters = apikeys_steps.counter_initialization(scenario['processed'], token, key)
            apikeys_steps.process_all_stats(scenario['processed'], counters, oper_uid)
        elif 'processed' not in scenario and 'raw' in scenario:
            apikeys_steps.counter_initialization(scenario['raw'], token, key)
            apikeys_steps.process_stats_raw(scenario['raw'], token, key)
        elif 'processed' in scenario and 'raw' in scenario:
            counters = apikeys_steps.counter_initialization(scenario['processed'], token, key)
            apikeys_steps.process_mixed_stats_dict(scenario, counters, oper_uid, token, key, link_id)
        else:
            raise ValueError(
                "Invalid scenario format. " +
                "The scenario must have processed or raw section or both. Given scenario: {}".format(
                    str(scenario)))

        link_id = apikeys_api.TEST().mongo_find('project_service_link', {"project_id": project_id})[0]['_id']
        result = apikeys_api.TEST().run_limit_checker(link_id=link_id)
        # limit_checker results asserts
        assert result.status_code == 200  # is status code 200
        assert len(json.loads(result.content)["limit_checker"]) > 0  # is response from limit checker
        assert json.loads(result.content)["result"] != expected_banned  # is banned status service response expected

        key_service_config = apikeys_api.TEST().mongo_find('project_service_link', {"project_id": project_id})
        assert key_service_config[0]["config"]["banned"] == expected_banned  # is banned status valid in the db
        assert key_service_config[0]['config']['approved'] == True
        if key_service_config[0]["config"]["banned"]:
            limit_config_list = apikeys_steps.get_limit_configs_by_service_id(key_service_config[0]['service_id'])
            assert long(key_service_config[0]['config']['ban_reason_id']) in [long(limit['lock_reason']) for limit in
                                                                              limit_config_list]

        orders = db.get_order_by_client(client_id)
        assert len(orders) > 0  # check if we have orders

    @staticmethod
    def autolock_autounlock(scenario, free_passport, service_cc, expected_banned):
        oper_uid, login, token, service_id, key, client_id, person_id = apikeys_steps.prepare_key(free_passport,
                                                                                                  service_cc)

        apikeys_steps.approve_key(key)

        project_id = apikeys_steps.get_project_id_by_key(key)
        link_id = apikeys_steps.get_link_by_key(key)[0]['link_id']

        result = apikeys_steps.process_stats_for_limit_cheker(key, oper_uid, scenario, project_id, token, link_id)

        Verifier().assert_limit_checker(result, True)
        Verifier().assert_project(project_id, True)
        Verifier().assert_check_key(True, key, token)
        Verifier().assert_order_status(client_id)

        apikeys_steps.update_limit_checker_stats_date(key, scenario, token,
                                                      BASE_DT - shift(
                                                          days=1))  # move limitchecker stats to yesterday

        apikeys_steps.remove_events_by_key(key)

        link_id = apikeys_api.TEST().mongo_find('project_service_link', {"project_id": project_id})[0]['_id']
        result = apikeys_api.TEST().run_limit_checker(link_id=link_id).json()

        Verifier().assert_limit_checker(result, False)
        Verifier().assert_project(project_id, False)
        Verifier().assert_check_key(False, key, token)
        Verifier().assert_order_status(client_id)

        result = apikeys_steps.process_stats_for_limit_cheker(key, oper_uid, scenario, project_id, token, link_id)

        Verifier().assert_limit_checker(result, expected_banned)
        Verifier().assert_project(project_id, True)
        Verifier().assert_check_key(True, key, token)
        Verifier().assert_order_status(client_id)

    @staticmethod
    def autolock_autounlock_unblockable(scenario, free_passport, service_cc, expected_banned):
        oper_uid, login, token, service_id, key, client_id, person_id = apikeys_steps.prepare_key(free_passport,
                                                                                                  service_cc)

        apikeys_steps.approve_key(key)

        apikeys_api.BO.update_unblockable(ADMIN, key, service_id, True)

        project_id = apikeys_steps.get_project_id_by_key(key)
        link_id = apikeys_steps.get_link_by_key(key)[0]['link_id']

        result = apikeys_steps.process_stats_for_limit_cheker(key, oper_uid, scenario, project_id, token, link_id)

        Verifier().assert_limit_checker(result, True)
        Verifier().assert_project(project_id, True)
        Verifier().assert_check_key(False, key, token)
        Verifier().assert_order_status(client_id)

        apikeys_steps.update_limit_checker_stats_date(key, scenario, token,
                                                      BASE_DT - shift(
                                                          days=1))  # move limitchecker stats to yesterday
        apikeys_steps.remove_events_by_key(key)

        link_id = apikeys_api.TEST().mongo_find('project_service_link', {"project_id": project_id})[0]['_id']
        result = apikeys_api.TEST().run_limit_checker(link_id=link_id).json()

        Verifier().assert_limit_checker(result, False)
        Verifier().assert_project(project_id, False)
        Verifier().assert_check_key(False, key, token)
        Verifier().assert_order_status(client_id)

        result = apikeys_steps.process_stats_for_limit_cheker(key, oper_uid, scenario, project_id, token, link_id)

        Verifier().assert_limit_checker(result, expected_banned)
        Verifier().assert_project(project_id, True)
        Verifier().assert_check_key(False, key, token)
        Verifier().assert_order_status(client_id)

    @staticmethod
    def autolock_manualunlock(scenario, free_passport, service_cc, expected_banned):
        oper_uid, login, token, service_id, key, client_id, person_id = apikeys_steps.prepare_key(free_passport,
                                                                                                  service_cc)

        apikeys_steps.approve_key(key)

        project_id = apikeys_steps.get_project_id_by_key(key)
        link_id = apikeys_steps.get_link_by_key(key)[0]['link_id']
        result = apikeys_steps.process_stats_for_limit_cheker(key, oper_uid, scenario, project_id, token, link_id)

        Verifier().assert_limit_checker(result, True)
        Verifier().assert_project(project_id, True)
        Verifier().assert_check_key(True, key, token)
        Verifier().assert_order_status(client_id)

        unlock_resons = apikeys_steps.get_service_unlock_reasons(service_id)
        auto_unlock_reasons = apikeys_steps.get_limit_config_unlock_reasons_by_service_id(service_id)
        manual_unlock_reasons = list(set(unlock_resons).symmetric_difference(auto_unlock_reasons))
        assert len(manual_unlock_reasons) > 0
        apikeys_api.BO.update_ban(ADMIN, key, service_id, False, manual_unlock_reasons[0])

        result = apikeys_steps.process_stats_for_limit_cheker(key, oper_uid, scenario, project_id, token, link_id)
        Verifier().assert_limit_checker(result, expected_banned)
        Verifier().assert_project(project_id, False)
        Verifier().assert_check_key(False, key, token)
        Verifier().assert_order_status(client_id)

    @staticmethod
    def manuallock_autounlock(scenario, free_passport, service_cc, expected_banned):
        oper_uid, login, token, service_id, key, client_id, person_id = apikeys_steps.prepare_key(free_passport,
                                                                                                  service_cc)

        apikeys_steps.approve_key(key)
        Verifier().assert_check_key(False, key, token)
        project_id = apikeys_steps.get_project_id_by_key(key)
        link_id = apikeys_steps.get_link_by_key(key)[0]['link_id']

        lock_resons = apikeys_steps.get_service_lock_reasons(service_id)
        auto_lock_reasons = apikeys_steps.get_limit_config_lock_reasons_by_service_id(service_id)
        manual_lock_reasons = list(set(lock_resons).symmetric_difference(auto_lock_reasons))
        assert len(manual_lock_reasons) > 0

        apikeys_api.BO.update_ban(ADMIN, key, service_id, True, manual_lock_reasons[0])

        result = apikeys_steps.process_stats_for_limit_cheker(key, oper_uid, scenario, project_id,
                                                              token,
                                                              link_id)  # add stats and run limit checker -> run limit checker?
        Verifier().assert_limit_checker(result, True)
        Verifier().assert_project(project_id, True)
        Verifier().assert_check_key(True, key, token)
        Verifier().assert_order_status(client_id)

        apikeys_steps.update_limit_checker_stats_date(key, scenario, token,
                                                      BASE_DT - shift(
                                                          days=1))  # move limitchecker stats to yesterday
        apikeys_steps.remove_events_by_key(key)

        link_id = apikeys_api.TEST().mongo_find('project_service_link', {"project_id": project_id})[0]['_id']
        result = apikeys_api.TEST().run_limit_checker(link_id=link_id).json()  # run limit checker

        Verifier().assert_limit_checker(result, False)
        Verifier().assert_project(project_id, True)
        Verifier().assert_check_key(True, key, token)
        Verifier().assert_order_status(client_id)

        result = apikeys_steps.process_stats_for_limit_cheker(key, oper_uid, scenario, project_id,
                                                              token, link_id)  # add stats and run limit checker
        Verifier().assert_limit_checker(result, expected_banned)
        Verifier().assert_project(project_id, True)
        Verifier().assert_check_key(True, key, token)
        Verifier().assert_order_status(client_id)

    @staticmethod
    def manuallock_autounlock_unblockable(scenario, free_passport, service_cc, expected_banned):
        oper_uid, login, token, service_id, key, client_id, person_id = apikeys_steps.prepare_key(free_passport,
                                                                                                  service_cc)

        apikeys_api.BO.update_unblockable(ADMIN, key, service_id, True)

        apikeys_steps.approve_key(key)
        Verifier().assert_check_key(False, key, token)
        project_id = apikeys_steps.get_project_id_by_key(key)
        link_id = apikeys_steps.get_link_by_key(key)[0]['link_id']

        lock_resons = apikeys_steps.get_service_lock_reasons(service_id)
        auto_lock_reasons = apikeys_steps.get_limit_config_lock_reasons_by_service_id(service_id)
        manual_lock_reasons = list(set(lock_resons).symmetric_difference(auto_lock_reasons))
        assert len(manual_lock_reasons) > 0

        apikeys_api.BO.update_ban(ADMIN, key, service_id, True, manual_lock_reasons[0])

        result = apikeys_steps.process_stats_for_limit_cheker(key, oper_uid, scenario, project_id,
                                                              token,
                                                              link_id)  # add stats and run limit checker -> run limit checker?
        Verifier().assert_limit_checker(result, True)
        Verifier().assert_project(project_id, True)
        Verifier().assert_check_key(False, key, token)
        Verifier().assert_order_status(client_id)

        apikeys_steps.update_limit_checker_stats_date(key, scenario, token,
                                                      BASE_DT - shift(
                                                          days=1))  # move limitchecker stats to yesterday
        apikeys_steps.remove_events_by_key(key)

        link_id = apikeys_api.TEST().mongo_find('project_service_link', {"project_id": project_id})[0]['_id']
        result = apikeys_api.TEST().run_limit_checker(link_id=link_id).json()  # run limit checker

        Verifier().assert_limit_checker(result, False)
        Verifier().assert_project(project_id, True)
        Verifier().assert_check_key(False, key, token)
        Verifier().assert_order_status(client_id)

        result = apikeys_steps.process_stats_for_limit_cheker(key, oper_uid, scenario, project_id,
                                                              token, link_id)  # add stats and run limit checker
        Verifier().assert_limit_checker(result, expected_banned)
        Verifier().assert_project(project_id, True)
        Verifier().assert_check_key(False, key, token)
        Verifier().assert_order_status(client_id)

    @staticmethod
    def manuallock_unblockable_update_to_blockable(scenario, free_passport, service_cc, expected_banned):
        oper_uid, login, token, service_id, key, client_id, person_id = apikeys_steps.prepare_key(free_passport,
                                                                                                  service_cc)

        apikeys_api.BO.update_unblockable(ADMIN, key, service_id, True)

        apikeys_steps.approve_key(key)
        Verifier().assert_check_key(False, key, token)
        project_id = apikeys_steps.get_project_id_by_key(key)
        link_id = apikeys_steps.get_link_by_key(key)[0]['link_id']

        lock_resons = apikeys_steps.get_service_lock_reasons(service_id)
        auto_lock_reasons = apikeys_steps.get_limit_config_lock_reasons_by_service_id(service_id)
        manual_lock_reasons = list(set(lock_resons).symmetric_difference(auto_lock_reasons))
        assert len(manual_lock_reasons) > 0

        apikeys_api.BO.update_ban(ADMIN, key, service_id, True, manual_lock_reasons[0])

        result = apikeys_steps.process_stats_for_limit_cheker(key, oper_uid, scenario, project_id,
                                                              token,
                                                              link_id)  # add stats and run limit checker -> run limit checker?
        Verifier().assert_limit_checker(result, True)
        Verifier().assert_project(project_id, True)
        Verifier().assert_check_key(False, key, token)
        Verifier().assert_order_status(client_id)

        apikeys_api.BO.update_unblockable(ADMIN, key, service_id, False)

        link_id = apikeys_api.TEST().mongo_find('project_service_link', {"project_id": project_id})[0]['_id']
        result = apikeys_api.TEST().run_limit_checker(
            link_id=link_id).json()  # add stats and run limit checker -> run limit checker?
        Verifier().assert_limit_checker(result, expected_banned)
        Verifier().assert_project(project_id, True)
        Verifier().assert_check_key(True, key, token)
        Verifier().assert_order_status(client_id)

    @staticmethod
    def manuallock_manualunlock(scenario, free_passport, service_cc, expected_banned):
        oper_uid, login, token, service_id, key, client_id, person_id = apikeys_steps.prepare_key(free_passport,
                                                                                                  service_cc)
        apikeys_steps.approve_key(key)
        Verifier().assert_check_key(False, key, token)
        project_id = apikeys_steps.get_project_id_by_key(key)
        link_id = apikeys_steps.get_link_by_key(key)[0]['link_id']

        lock_resons = apikeys_steps.get_service_lock_reasons(service_id)
        auto_lock_reasons = apikeys_steps.get_limit_config_lock_reasons_by_service_id(service_id)
        manual_lock_reasons = list(set(lock_resons).symmetric_difference(auto_lock_reasons))
        assert len(manual_lock_reasons) > 0

        apikeys_api.BO.update_ban(ADMIN, key, service_id, True, manual_lock_reasons[0])

        result = apikeys_steps.process_stats_for_limit_cheker(key, oper_uid, scenario, project_id, token, link_id)
        Verifier().assert_limit_checker(result, True)
        Verifier().assert_project(project_id, True)
        Verifier().assert_check_key(True, key, token)
        Verifier().assert_order_status(client_id)

        apikeys_steps.update_limit_checker_stats_date(key, scenario, token,
                                                      BASE_DT - shift(
                                                          days=1))  # move limitchecker stats to yesterday
        apikeys_steps.remove_events_by_key(key)

        link_id = apikeys_api.TEST().mongo_find('project_service_link', {"project_id": project_id})[0]['_id']
        result = apikeys_api.TEST().run_limit_checker(link_id=link_id).json()

        Verifier().assert_limit_checker(result, False)
        Verifier().assert_project(project_id, True)
        Verifier().assert_check_key(True, key, token)
        Verifier().assert_order_status(client_id)

        unlock_resons = apikeys_steps.get_service_unlock_reasons(service_id)
        auto_unlock_reasons = apikeys_steps.get_limit_config_unlock_reasons_by_service_id(service_id)
        manual_unlock_reasons = list(set(unlock_resons).symmetric_difference(auto_unlock_reasons))
        assert len(manual_unlock_reasons) > 0

        apikeys_api.BO.update_ban(ADMIN, key, service_id, False, manual_unlock_reasons[0])

        result = apikeys_steps.process_stats_for_limit_cheker(key, oper_uid, scenario, project_id, token, link_id)
        Verifier().assert_limit_checker(result, expected_banned)
        Verifier().assert_project(project_id, expected_banned)
        Verifier().assert_check_key(expected_banned, key, token)
        Verifier().assert_order_status(client_id)

    @staticmethod
    def manuallock_update_to_autolock(scenario, free_passport, service_cc, expected_banned):
        oper_uid, login, token, service_id, key, client_id, person_id = apikeys_steps.prepare_key(free_passport,
                                                                                                  service_cc)
        apikeys_steps.approve_key(key)
        Verifier().assert_check_key(False, key, token)
        project_id = apikeys_steps.get_project_id_by_key(key)
        link_id = apikeys_steps.get_link_by_key(key)[0]['link_id']

        lock_resons = apikeys_steps.get_service_lock_reasons(service_id)
        auto_lock_reasons = apikeys_steps.get_limit_config_lock_reasons_by_service_id(service_id)
        manual_lock_reasons = list(set(lock_resons).symmetric_difference(auto_lock_reasons))
        assert len(manual_lock_reasons) > 0

        apikeys_api.BO.update_ban(ADMIN, key, service_id, True, manual_lock_reasons[0])

        result = apikeys_steps.process_stats_for_limit_cheker(key, oper_uid, scenario, project_id, token, link_id)
        Verifier().assert_limit_checker(result, True)
        Verifier().assert_project(project_id, True)
        Verifier().assert_check_key(True, key, token)
        Verifier().assert_order_status(client_id)

        apikeys_steps.update_limit_checker_stats_date(key, scenario, token,
                                                      BASE_DT - shift(
                                                          days=1))  # move limitchecker stats to yesterday
        apikeys_steps.remove_events_by_key(key)

        link_id = apikeys_api.TEST().mongo_find('project_service_link', {"project_id": project_id})[0]['_id']
        result = apikeys_api.TEST().run_limit_checker(link_id=link_id).json()

        Verifier().assert_limit_checker(result, False)
        Verifier().assert_project(project_id, True)
        Verifier().assert_check_key(True, key, token)
        Verifier().assert_order_status(client_id)

        # manual unlock -> auto unlock
        auto_unlock_reasons = apikeys_steps.get_limit_config_unlock_reasons_by_service_id(service_id)
        assert len(auto_unlock_reasons) > 0
        apikeys_steps.update_key_service_config_lock_reason_by_project_id(project_id, auto_unlock_reasons[0])
        limit_config_ids = apikeys_steps.get_limit_config_id_by_service_id(service_id)
        memo_message = '[LIMIT] {}'.format(limit_config_ids[0])
        assert len(memo_message) > 0
        apikeys_steps.update_key_service_config_ban_memo_by_project_id(project_id, memo_message)
        apikeys_steps.delete_key_service_config_ban_by_project_id(project_id)
        # section end

        apikeys_steps.update_limit_checker_stats_date(key, scenario, token,
                                                      BASE_DT - shift(
                                                          days=1))  # move limitchecker stats to yesterday
        apikeys_steps.remove_events_by_key(key)

        result = apikeys_api.TEST().run_limit_checker(link_id=link_id).json()

        Verifier().assert_limit_checker(result, False)
        Verifier().assert_project(project_id, False)
        Verifier().assert_check_key(False, key, token)
        Verifier().assert_order_status(client_id)

        result = apikeys_steps.process_stats_for_limit_cheker(key, oper_uid, scenario, project_id, token, link_id)

        Verifier().assert_limit_checker(result, expected_banned)
        Verifier().assert_project(project_id, True)
        Verifier().assert_check_key(True, key, token)
        Verifier().assert_order_status(client_id)

    @staticmethod
    def autolock_manuallock(scenario, free_passport, service_cc, expected_banned):
        oper_uid, login, token, service_id, key, client_id, person_id = apikeys_steps.prepare_key(free_passport,
                                                                                                  service_cc)

        apikeys_steps.approve_key(key)

        project_id = apikeys_steps.get_project_id_by_key(key)
        link_id = apikeys_steps.get_link_by_key(key)[0]['link_id']

        result = apikeys_steps.process_stats_for_limit_cheker(key, oper_uid, scenario, project_id, token, link_id)

        Verifier().assert_limit_checker(result, True)
        Verifier().assert_project(project_id, True)
        Verifier().assert_check_key(True, key, token)
        Verifier().assert_order_status(client_id)

        # update to manual lock section
        lock_resons = apikeys_steps.get_service_lock_reasons(service_id)
        auto_lock_reasons = apikeys_steps.get_limit_config_lock_reasons_by_service_id(service_id)
        manual_lock_reasons = list(set(lock_resons).symmetric_difference(auto_lock_reasons))
        apikeys_steps.update_key_service_config_lock_reason_by_project_id(project_id, manual_lock_reasons[0])
        memo_message = 'manual ban'
        apikeys_steps.update_key_service_config_ban_memo_by_project_id(project_id, memo_message)
        # manual lock section end

        apikeys_steps.update_limit_checker_stats_date(key, scenario, token,
                                                      BASE_DT - shift(
                                                          days=1))  # move limitchecker stats to yesterday
        apikeys_steps.remove_events_by_key(key)

        link_id = apikeys_api.TEST().mongo_find('project_service_link', {"project_id": project_id})[0]['_id']
        result = apikeys_api.TEST().run_limit_checker(link_id=link_id).json()

        Verifier().assert_limit_checker(result, False)
        Verifier().assert_project(project_id, True)
        Verifier().assert_check_key(True, key, token)
        Verifier().assert_order_status(client_id)

    @staticmethod
    def non_commercial(scenario, free_passport, service_cc, tariff):
        if hasattr(scenario, 'stats'):
            keys_count = len({x.get('key') for x in scenario.stats}) if scenario.stats[0].get('key') else 1
        else:
            keys_count = 1

        # COMPLEX. Генерируем пары ключ-сервис. Создаем плательщика, клиента.
        oper_uid, login, token, service_id, keys, client_id, person_id = \
            apikeys_steps.prepare_several_keys(free_passport, service_cc, keys_count, keys_count)
        link_id = apikeys_steps.get_link_by_key(keys.linked[0])[0]['link_id']
        project_id = apikeys_steps.get_project_id_by_key(keys.linked[0])
        apikeys_api.BO.schedule_tariff_changing(ADMIN, keys.linked[0], service_id, tariff.tariff_cc)
        apikeys_api.BO.update_service_link(ADMIN, keys.linked[0], service_id, 'patch',
                                           '{{"{}":{{"limit":{}}}}}'.format(tariff.unit, scenario.limit))
        apikeys_api.TEST.run_tarifficator(link_id)
        wait(apikeys_steps.check_limit_activation, equal_to(scenario.limit),
             timeout=W.time, sleep_time=W.s_time)(ADMIN, keys.linked[0], service_id, tariff.unit)
        scenario.stats = plain.completions_shift_limit(scenario.stats, scenario.limit, tariff.counters)
        dates = sorted([row.get('dt') for row in scenario.stats])
        first_date = dates[0] - shift(hours=1)
        last_date = dates[-1] + shift(hours=1)
        apikeys_steps.change_and_check_tarifficator_activated_date(link_id, oper_uid, project_id, first_date)
        scenario = apikeys_steps.add_counters_to_scenario(scenario, token, keys.linked)
        apikeys_steps.process_every_stat(scenario, oper_uid, link_id,None)
        apikeys_api.TEST.run_limit_checker(link_id)
        if hasattr(scenario, 'active_after_scenario'):
            utils.check_that(
                apikeys_api.API.get_link_info(token, apikeys_steps.get_keys_by_uid(oper_uid, project_id, service_id)[0]).json()['active'],
                mtch.equal_to(scenario.active_after_scenario), u'Выполняем проверку связки после завершения сценария')
        if hasattr(scenario, 'validity_period'):
            apikeys_api.TEST.run_tarifficator(link_id, last_date + shift(days=scenario.validity_period))
            utils.check_that(
                apikeys_api.API.get_link_info(token, apikeys_steps.get_keys_by_uid(oper_uid, project_id, service_id)[0]).json()['active'],
                mtch.equal_to(True),
                u'Выполняем проверку отсутствия ограничений по сроку действия тарифа')


class Postpayment():
    @staticmethod
    def basic(scenario, db_connection, service_cc, tariff_price=None):
        keys_count = len({x.get('key') for x in scenario.stats}) if len({x.get('key') for x in scenario.stats}) else 1
        oper_uid, login, token, service_id, keys, client_id, person_id = apikeys_steps.prepare_several_keys(
            db_connection,
            service_cc,
            keys_count,
            keys_count)
        apikeys_api.BO().get_client_from_balance(ADMIN, oper_uid)
        contract_id = apikeys_steps.create_active_postpay_contract(scenario,
                                                                   service_cc,
                                                                   client_id,
                                                                   person_id,
                                                                   scenario.signed_date)

        project_id = apikeys_steps.get_project_id_by_key(keys.linked[0])
        link_id = apikeys_steps.get_link_by_key(keys.linked[0])[0]['link_id']
        apikeys_api.TEST().run_user_contractor(oper_uid)
        db_connection['task'].delete_many({'link':link_id})
        apikeys_api.TEST.run_tarifficator(link_id, allow_not_200=True)
        apikeys_steps.change_and_check_tarifficator_activated_date(link_id, oper_uid, project_id, scenario.signed_date)

        if scenario.stats:
            scenario = apikeys_steps.add_counters_to_scenario(scenario, token, keys.linked)

            apikeys_steps.process_every_stat(scenario, oper_uid, link_id,db_connection)

        else:
            # двойной вызов run_user_contractor с первой датой
            # костыль, пока не прокидывается on_date в механизм смены тарифа
            apikeys_api.TEST().run_tarifficator(link_id, (scenario.base_dt + shift(hours=1)))
            apikeys_api.TEST().run_tarifficator(link_id, (scenario.base_dt + shift(hours=2)))

            apikeys_api.TEST().run_tarifficator(link_id, (scenario.base_dt + shift(days=1)))

        # --------- Assert shipments ----------
        orders = db.get_order_by_client(client_id)
        print '[DEBUG] ...... Orders: {}'.format(orders)
        if scenario.expected == 0:
            utils.check_that(orders, (mtch.equal_to([])), 'Выполняем проверку, что список заказов пуст')
        else:
            apikeys_steps.check_order(orders, scenario.expected)
            if hasattr(scenario, 'close_month') and scenario.close_month:
                apikeys_steps.close_month(client_id, scenario.expected, orders,
                                          scenario.base_dt + shift(days=40), contract_id=contract_id,
                                          scaling_expected=tariff_price)


    @staticmethod
    def faxed_signed(scenario, db_connection, service_cc):
        keys_count = 1
        oper_uid, login, token, service_id, keys, client_id, person_id = apikeys_steps.prepare_several_keys(
            db_connection,
            service_cc,
            keys_count,
            keys_count)
        faxed_scenario = utils.aDict(scenario.faxed)
        faxed_scenario.tariff = scenario.tariff
        keys_copy = keys.linked[:]
        params = {'CLIENT_ID': client_id,
                  'PERSON_ID': person_id,
                  'APIKEYS_TARIFFS': apikeys_steps.get_apikeys_tariffs(scenario, service_cc),
                  'FINISH_DT': (faxed_scenario.base_dt + shift(days=365)).strftime('%Y-%m-%dT00:00:00'),
                  'IS_FAXED': faxed_scenario.is_faxed.strftime('%Y-%m-%dT00:00:00'),
                  'IS_SIGNED': None,
                  }
        contract_id, _ = steps.ContractSteps.create_contract('no_agency_apikeys_post', params)
        link_id = apikeys_steps.get_link_by_key(keys.linked[0])[0]['link_id']
        apikeys_api.BO().get_client_from_balance(ADMIN, oper_uid)
        apikeys_api.TEST().run_user_contractor(oper_uid)
        db_connection['task'].delete_many({'link':link_id})
        apikeys_api.TEST.run_tarifficator(link_id, allow_not_200=True)
        project_id = apikeys_steps.get_project_id_by_key(keys.linked[0])
        faxed_scenario = apikeys_steps.add_counters_to_scenario(faxed_scenario, token, keys.linked)
        apikeys_api.TEST().run_user_contractor(oper_uid)

        apikeys_steps.change_and_check_tarifficator_activated_date(link_id, oper_uid, project_id,
                                                                   faxed_scenario.is_faxed)

        apikeys_steps.process_every_stat(faxed_scenario, oper_uid, link_id,db_connection)
        signed_scenario = utils.aDict(scenario.signed)
        signed_scenario.tariff = scenario.tariff
        db.oracle_update('t_contract_collateral',
                         {'is_signed': "TO_DATE('{0}', 'YYYY-MM-DD HH24:MI:SS')".format(
                             signed_scenario.is_signed.strftime('%Y-%m-%d 00:00:00'))},
                         {'contract2_id': contract_id, 'num': None})

        apikeys_api.BO().get_client_from_balance(ADMIN, oper_uid)
        apikeys_api.TEST().run_user_contractor(oper_uid)

        signed_scenario = apikeys_steps.add_counters_to_scenario(signed_scenario, token, keys_copy)
        apikeys_steps.process_every_stat(signed_scenario, oper_uid, link_id,db_connection)

        orders = db.get_order_by_client(client_id)
        print '[DEBUG] ...... Orders: {}'.format(orders)
        if scenario.expected == 0:
            utils.check_that(orders, (mtch.equal_to([])), 'Выполняем проверку, что список заказов пуст')

        else:
            apikeys_steps.check_order(orders, scenario.expected)
            add_months_to_closing = 0 if hasattr(scenario, 'close_one_month') else 1
            if hasattr(scenario, 'one_month_expected'):
                scenario.expected = scenario.one_month_expected
            if hasattr(scenario, 'close_month') and scenario.close_month:
                apikeys_steps.close_month(client_id, scenario.expected, orders,
                                          faxed_scenario.base_dt + shift(days=70), add_months=add_months_to_closing,
                                          contract_id=contract_id)

    @staticmethod
    def trial(scenario, service_cc, db_connection):
        keys_count = len({x.get('key') for x in scenario.stats}) if len({x.get('key') for x in scenario.stats}) else 1
        oper_uid, login, token, service_id, keys, client_id, person_id = apikeys_steps.prepare_several_keys(
            db_connection,
            service_cc,
            keys_count,
            keys_count)
        # apikeys_steps.create_active_postpay_contract(scenario,
        #                                              service_cc,
        #                                              client_id,
        #                                              person_id,
        #                                              scenario.signed_date)

        apikeys_api.BO().get_client_from_balance(ADMIN, oper_uid)
        key_for_check = keys.linked[0]
        project_id = apikeys_steps.get_project_id_by_key(keys.linked[0])
        link_id = apikeys_steps.get_link_by_key(keys.linked[0])[0]['link_id']
        apikeys_api.TEST().run_user_contractor(oper_uid)
        db_connection['task'].delete_many({'link': link_id})
        apikeys_steps.change_and_check_tarifficator_activated_date(link_id, oper_uid, project_id, scenario.base_dt)

        scenario = apikeys_steps.add_counters_to_scenario(scenario, token, keys.linked)

        apikeys_steps.process_every_stat(scenario, oper_uid, link_id,db_connection)
        apikeys_api.TEST().run_tarifficator(link_id)

        # --------- Assert shipments ----------
        wait(apikeys_steps.check_link_activation,
             is_(scenario.active_after_stats), timeout=W.time, sleep_time=W.s_time)(token, key_for_check)
        wait(apikeys_steps.check_key_activation,
             is_(scenario.active_after_stats), timeout=W.time, sleep_time=W.s_time)(token, key_for_check)
        assert not db.get_order_by_client(client_id), 'Проверяем отсутствие закозов'

    @staticmethod
    def trial_to_paid(scenario, free_passport, tariff_price=None):
        trial_scenario = scenario.trial
        service_cc = trial_scenario.service_cc

        oper_uid, login, token, service_id, key, client_id, person_id = apikeys_steps.prepare_key(
            free_passport,
            service_cc)
        project_id = apikeys_steps.get_project_id_by_key(key)
        link_id = apikeys_steps.get_link_by_key(key)[0]['link_id']
        apikeys_steps.change_and_check_tarifficator_activated_date(link_id, oper_uid, project_id,
                                                                   trial_scenario.base_dt)

        trial_scenario = apikeys_steps.add_counters_to_scenario(trial_scenario, token, key)

        apikeys_steps.process_every_stat(trial_scenario, oper_uid, link_id,None)

        # --------- Assert shipments ----------
        wait(apikeys_steps.check_link_activation,
             is_(trial_scenario.active_after_stats), timeout=W.time, sleep_time=W.s_time)(token, key)
        wait(apikeys_steps.check_key_activation,
             is_(trial_scenario.active_after_stats), timeout=W.time, sleep_time=W.s_time)(token, key)
        assert not db.get_order_by_client(client_id), 'Проверяем отсутствие закозов'

        paid_scenario = scenario.paid

        contract_id = apikeys_steps.create_active_postpay_contract(paid_scenario,
                                                                   service_cc,
                                                                   client_id,
                                                                   person_id,
                                                                   paid_scenario.signed_date)
        project_id = apikeys_steps.get_project_id_by_key(key)
        link_id = apikeys_steps.get_link_by_key(key)[0]['link_id']
        apikeys_api.TEST().run_user_contractor(oper_uid)
        apikeys_steps.change_and_check_tarifficator_activated_date(link_id, oper_uid, project_id,
                                                                   paid_scenario.signed_date)
        apikeys_api.TEST().run_tarifficator(link_id, paid_scenario.signed_date)

        if paid_scenario.stats:
            paid_scenario = apikeys_steps.add_counters_to_scenario(paid_scenario, token, key)

            apikeys_steps.process_every_stat(paid_scenario, oper_uid, link_id,None)

        else:
            # двойной вызов run_user_contractor с первой датой
            # костыль, пока не прокидывается on_date в механизм смены тарифа
            apikeys_api.TEST().run_tarifficator(link_id, (paid_scenario.base_dt + shift(hours=1)))
            apikeys_api.TEST().run_tarifficator(link_id, (paid_scenario.base_dt + shift(hours=2)))

            apikeys_api.TEST().run_tarifficator(link_id, (paid_scenario.base_dt + shift(days=1)))

        # --------- Assert shipments ----------
        orders = db.get_order_by_client(client_id)
        print '[DEBUG] ...... Orders: {}'.format(orders)
        if paid_scenario.expected == 0:
            utils.check_that(orders, (mtch.equal_to([])), 'Выполняем проверку, что список заказов пуст')

        else:
            apikeys_steps.check_order(orders, paid_scenario.expected)
            add_months_to_closing = 0 if hasattr(scenario, 'close_one_month') else 1
            if hasattr(scenario, 'one_month_expected'):
                scenario.expected = paid_scenario.one_month_expected
            if hasattr(scenario, 'close_month') and scenario.close_month:
                apikeys_steps.close_month(client_id, paid_scenario.expected, orders,
                                          paid_scenario.base_dt + shift(days=70), contract_id=contract_id,
                                          add_months=add_months_to_closing, scaling_expected=tariff_price)

    @staticmethod
    def custom(scenario, db_connection, service_cc):
        keys_count = len({x.get('key') for x in scenario.stats}) if len({x.get('key') for x in scenario.stats}) else 1
        oper_uid, login, token, service_id, keys, client_id, person_id = apikeys_steps.prepare_several_keys(
            db_connection,
            service_cc,
            keys_count,
            keys_count)

        key_for_check = keys.linked[0]
        project_id = apikeys_steps.get_project_id_by_key(keys.linked[0])
        link_id = apikeys_steps.get_link_by_key(keys.linked[0])[0]['link_id']
        apikeys_api.BO().schedule_tariff_changing(ADMIN, key_for_check, service_id,
                                                  scenario.tariff.replace('apikeys_', ''))
        time.sleep(2)
        # apikeys_steps.change_scheduled_tariff_date(project_id, BASE_DT)

        scenario = apikeys_steps.add_counters_to_scenario(scenario, token, keys.linked)

        apikeys_steps.process_every_stat(scenario, oper_uid, link_id,db_connection)
        apikeys_api.TEST().run_limit_checker(link_id=link_id)
        apikeys_api.TEST().run_tarifficator(link_id)
        # --------- Assert shipments ----------
        wait(apikeys_steps.check_link_activation,
             is_(scenario.active_after_stats), timeout=W.time, sleep_time=W.s_time)(token, key_for_check)
        wait(apikeys_steps.check_key_activation,
             is_(scenario.active_after_stats), timeout=W.time, sleep_time=W.s_time)(token, key_for_check)
        utils.check_that(db.get_order_by_client(client_id), mtch.equal_to([]), 'Проверяем отсутствие заказов')


class PostpaymentWithPrepaymentPeriod:
    @staticmethod
    def basic(scenario, db_connection, service_cc, tariff):
        keys_count = len({x.get('key') for x in scenario.stats}) if scenario.stats[0].get('key') else 1
        if hasattr(tariff, 'day_limit'):
            scenario.stats = plain.completions_shift_limit(scenario.stats, tariff.day_limit, tariff.counters)
        if hasattr(tariff, 'month_limit'):
            scenario.stats = plain.completions_shift_limit(scenario.stats, tariff.month_limit, tariff.counters)

        # COMPLEX. Генерируем пары ключ-сервис. Создаем плательщика, клиента в Балансе.
        oper_uid, login, token, service_id, keys, client_id, person_id = apikeys_steps.prepare_several_keys(
            db_connection,
            service_cc,
            keys_count,
            keys_count)

        # BALANCE_UI  Подключаем к проекту тариф, путем создания на него контракта
        contract_id, project_id = apikeys_steps.prepare_and_move_contract(client_id, keys.linked[0], oper_uid,
                                                                          person_id,
                                                                          scenario, service_cc, service_id, db_connection)

        link_id = apikeys_steps.get_link_by_key(keys.linked[0])[0]['link_id']
        apikeys_api.TEST.run_tarifficator(link_id)
        first_date = sorted([row.get('dt') for row in scenario.stats])[-1] - shift(hours=1)
        # INTERNAL Устанавливаем дату начала действия договора на начальную дату сценария
        apikeys_steps.change_and_check_tarifficator_activated_date(link_id, oper_uid, project_id, first_date)
        # APIKEYS_BACKEND Инициализируем счетчики
        scenario = apikeys_steps.add_counters_to_scenario(scenario, token, keys.linked)
        # APIKEYS_API Эмулируем отправку статистики
        apikeys_steps.process_every_stat(scenario, oper_uid, link_id,db_connection)

        orders = db.get_order_by_client(client_id)
        print '[DEBUG] ...... Orders: {}'.format(orders)
        orders.sort(key=lambda x: x['service_order_id'])
        days_order = orders[0]
        # INTERNAL  Получаем открутки
        shipments = db.get_shipments_by_service_order_id({'service_order_id': days_order['service_order_id'],
                                                          'service_id': days_order['service_id']})

        api.test_balance().Campaigns({'service_id': days_order['service_id'],
                                      'service_order_id': days_order['service_order_id'],
                                      'use_current_shipment': True})

        utils.check_that(D(shipments[0]['money']), (mtch.equal_to(D(tariff.year_price))),
                         u'Выполняем проверку суммы абонплаты')

        if scenario.over_limit and hasattr(tariff, 'price_over_limit_per_1000') and tariff.price_over_limit_per_1000:
            expected_money = D(plain.calculate_over_limit_expected(scenario.stats, tariff.day_limit,
                                                                   tariff.price_over_limit_per_1000))
            units_order = orders[1]
            shipments = db.get_shipments_by_service_order_id({'service_order_id': units_order['service_order_id'],
                                                              'service_id': units_order['service_id']})

            utils.check_that(D(shipments[0]['money']), (mtch.equal_to(D(expected_money))),
                             u'Выполняем проверку суммы перекруток')

        elif scenario.over_limit and hasattr(tariff, 'price_over_limit_per_1') and tariff.price_over_limit_per_1:
            expected_money = D(plain.calculate_over_limit_expected(scenario.stats, tariff.month_limit,
                                                                   tariff.price_over_limit_per_1, part=1))
            units_order = orders[1]
            shipments = db.get_shipments_by_service_order_id({'service_order_id': units_order['service_order_id'],
                                                              'service_id': units_order['service_id']})

            utils.check_that(D(shipments[0]['money']), (mtch.equal_to(D(expected_money))),
                             u'Выполняем проверку суммы перекруток')

        else:
            utils.check_that(len(orders), (mtch.equal_to(1)),
                             u'Выполняем проверку, что нет заказа по перекруткам')
            expected_money = 0
        if hasattr(tariff, 'ban') and hasattr(scenario, 'active_after_stats'):
            if not scenario.active_after_stats and tariff.ban:
                utils.check_that(
                    apikeys_api.API.get_link_info(token, apikeys_steps.get_keys_by_uid(oper_uid, project_id, service_id)[0]).json()['active'],
                    mtch.equal_to(False),
                    u'Выполняем проверку связки после завершения сценария')
            else:
                utils.check_that(
                    apikeys_api.API.get_link_info(token, apikeys_steps.get_keys_by_uid(oper_uid, project_id, service_id)[0]).json()['active'],
                    mtch.equal_to(True),
                    u'Выполняем проверку связки после завершения сценария')
        if hasattr(scenario, 'close_month') and scenario.close_month:
            expected_money += D(tariff.year_price)
            apikeys_steps.close_month(client_id, expected_money, orders, scenario.base_dt + shift(days=70),
                                      contract_id=contract_id)

    @staticmethod
    def free_to_paid(scenario, db_connection, service_cc, tariff):
        oper_uid, login, token, service_id, key, client_id, person_id = apikeys_steps.prepare_key(db_connection,
                                                                                                  service_cc)

        if tariff.free_counters.need_approve:
            apikeys_steps.approve_key(key)

        free_scenario = utils.aDict(scenario.free)
        free_scenario.stats = plain.completions_shift_limit(free_scenario.stats,
                                                            tariff.free_counters.limit,
                                                            tariff.free_counters.counters)

        free_scenario = apikeys_steps.add_counters_to_scenario(free_scenario, token, key)
        link_id = apikeys_steps.get_link_by_key(key)[0]['link_id']

        utils.check_that(apikeys_api.API.get_link_info(token, key).json()['active'], (mtch.equal_to(True)),
                         u'Выполняем проверку связки')
        project_id=db_connection['project'].find_one({'user_uid':oper_uid})['_id']
        first_date=sorted([row.get('dt') for row in free_scenario.stats])[0] - shift(hours=1)
        apikeys_steps.change_and_check_tarifficator_activated_date(link_id, oper_uid, project_id, first_date)
        apikeys_steps.process_every_stat(free_scenario, oper_uid, link_id,db_connection)
        apikeys_api.TEST().run_user_contractor(oper_uid)

        utils.check_that(len(db.get_order_by_client(client_id)), (mtch.equal_to(0)),
                         u'Выполняем проверку, что нет заказов')

        paid_scenario = utils.aDict(scenario.paid)
        paid_scenario.stats = plain.completions_shift_limit(paid_scenario.stats, tariff.day_limit, tariff.counters)

        contract_id, project_id = apikeys_steps.prepare_and_move_contract(client_id, key, oper_uid, person_id,
                                                                          paid_scenario, service_cc, service_id,db_connection)

        apikeys_api.TEST().run_user_contractor(oper_uid)
        link_id = apikeys_steps.get_link_by_key(key)[0]['link_id']
        first_date = sorted([row.get('dt') for row in paid_scenario.stats])[0] - shift(hours=1)
        apikeys_steps.change_and_check_tarifficator_activated_date(link_id, oper_uid, project_id, first_date)

        paid_scenario = apikeys_steps.add_counters_to_scenario(paid_scenario, token, key)
        apikeys_steps.process_every_stat(paid_scenario, oper_uid, link_id,db_connection)

        orders = db.get_order_by_client(client_id)
        orders.sort(key=lambda x: x['service_order_id'])
        print '[DEBUG] ...... Orders: {}'.format(orders)
        days_order = orders[0]

        shipments = db.get_shipments_by_service_order_id({'service_order_id': days_order['service_order_id'],
                                                          'service_id': days_order['service_id']})

        api.test_balance().Campaigns({'service_id': days_order['service_id'],
                                      'service_order_id': days_order['service_order_id'],
                                      'use_current_shipment': True})

        utils.check_that(D(shipments[0]['days']), (mtch.equal_to(D(tariff.year_price))),
                         u'Выполняем проверку суммы абонплаты')

        if paid_scenario.over_limit and tariff.price_over_limit_per_1000:
            expected_money = D(plain.calculate_over_limit_expected(paid_scenario.stats, tariff.day_limit,
                                                                   tariff.price_over_limit_per_1000))
            units_order = orders[1]
            shipments = db.get_shipments_by_service_order_id({'service_order_id': units_order['service_order_id'],
                                                              'service_id': units_order['service_id']})

            utils.check_that(D(shipments[0]['days']), (mtch.equal_to(D(expected_money))),
                             u'Выполняем проверку суммы перекруток')

        else:
            utils.check_that(len(orders), (mtch.equal_to(1)),
                             u'Выполняем проверку, что нет заказа по перекруткам')
            expected_money = 0

        if hasattr(scenario, 'close_month') and scenario.close_month:
            expected_money += tariff.year_price
            apikeys_steps.close_month(client_id, expected_money, orders, paid_scenario.base_dt + shift(days=70),
                                      contract_id=contract_id)

    @staticmethod
    def invoices(scenario, db_connection, service_cc, tariff):
        oper_uid, login, token, service_id, keys, client_id, person_id = apikeys_steps.prepare_several_keys(
            db_connection,
            service_cc,
            1, 1)
        contract_id, project_id = apikeys_steps.prepare_and_move_contract(client_id, keys.linked[0], oper_uid,
                                                                          person_id,
                                                                          scenario, service_cc, service_id,
                                                                          db_connection,move_days=0)
        link_id = apikeys_steps.get_link_by_key(keys.linked[0])[0]['link_id']
        apikeys_api.TEST.run_user_contractor(oper_uid)
        apikeys_api.TEST.run_tarifficator(link_id)
        # first_date = scenario.base_dt - shift(sorted([row.shift_days for row in scenario.shift_days_invoices])[-1])
        # apikeys_steps.change_and_check_tarifficator_activated_date(link_id, oper_uid, project_id, first_date)

        for item in scenario.shift_days_invoices:
            apikeys_api.TEST().run_tarifficator(link_id, on_date=scenario.base_dt + shift(days=item.shift_days))
            apikeys_steps.get_tarifficator_state(oper_uid)
            invoices = [invoice for invoice in db.get_invoices_by_contract_id(contract_id)
                        if invoice['type'] == 'prepayment']
            utils.check_that(len(invoices), greater_than_or_equal_to(item.invoices_amount),
                             u'Выполняем проверку количества инвойсов при base_dt + {}'.format(item.shift_days))
            for invoice in invoices:
                utils.check_that(invoice['effective_sum'], (mtch.equal_to(tariff.year_price)),
                                 u'Выполняем проверку суммы инвойса')


class Prepayment():
    @staticmethod
    def basic(scenario, db_connection, service_cc, tariff, person_type):
        if hasattr(scenario, 'stats'):
            keys_count = len({x.get('key') for x in scenario.stats}) if scenario.stats[0].get('key') else 1
        else:
            keys_count = 1

        # COMPLEX. Генерируем пары ключ-сервис. Создаем плательщика, клиента в Балансе.
        oper_uid, login, token, service_id, keys, client_id, person_id = \
            apikeys_steps.prepare_several_keys(db_connection, service_cc, keys_count, keys_count, person_type.type)

        tariff_cc = scenario.tariff.replace('apikeys_', '')

        # COMPLEX. Подключаем тариф и плательщика к связке ключ-сервис
        project_id, link_id = apikeys_steps.prepare_client_data_for_prepayment(keys.linked[0], oper_uid, service_id,
                                                                               person_id, tariff_cc,
                                                                               on_date=scenario.base_dt, db_connection=db_connection)
        key_for_check = keys.linked[0]
        money = apikeys_utils.get_deposit_money(scenario, tariff)
        if hasattr(scenario, 'over_limit') and scenario.active_after_scenario and scenario.over_limit:
            money += D(
                plain.calculate_over_limit_expected(scenario.stats, tariff.day_limit, tariff.price_over_limit_per_1000))
        # todo remove key param 'check_link_and_key' after fix APIKEYS-654

        # COMPLEX. Выставляем и оплачиваем счет в балансе напрямую на цену лицензии
        apikeys_steps.deposit_money(oper_uid, project_id, service_id, person_id, client_id, token, key_for_check, money,
                                    person_type.paysys_invoice, on_date=scenario.base_dt)

        if hasattr(scenario, 'need_turn_on_tariff') and scenario.need_turn_on_tariff:
            apikeys_api.UI.turn_on_tariff(oper_uid, project_id, service_id, tariff_cc)
            apikeys_api.TEST.run_tarifficator(link_id)

        # INTERNAL Вызываем тарификатор для списания средств и активации тарифа
        apikeys_api.TEST().run_tarifficator(link_id, scenario.base_dt, allow_not_200=True)
        apikeys_api.TEST().run_tarifficator(link_id, scenario.base_dt, allow_not_200=True)

        # INTERNAL Проверяем актиировался ли ключ и связка ключ-сервис
        wait(apikeys_steps.check_link_activation, is_(True), timeout=W.time, sleep_time=W.s_time)(token, key_for_check)
        wait(apikeys_steps.check_key_activation, is_(True), timeout=W.time, sleep_time=W.s_time)(token, key_for_check)

        if hasattr(scenario, 'stats') and scenario.stats:
            first_date = sorted([row.get('dt') for row in scenario.stats])[0] - shift(hours=1)
            # INTERNAL Пуск тарификатора для начальной даты
            apikeys_steps.change_and_check_tarifficator_activated_date(link_id, oper_uid, project_id, first_date)
            # COMPLEX Инициализируем счетчики и добавляем их к сценарию
            scenario = apikeys_steps.add_counters_to_scenario(scenario, token, keys.linked)
            # INTERNAL Вставка статистики и запуск тарификатора для всех последующих дат
            apikeys_steps.process_every_stat(scenario, oper_uid, link_id,db_connection)

        else:
            for day in scenario.tarifficator_days:
                apikeys_api.TEST().run_tarifficator(link_id, scenario.base_dt + shift(days=day))

                # todo remove second run_tarifficator after fix APIKEYS-567
                apikeys_api.TEST().run_tarifficator(link_id, scenario.base_dt + shift(days=day))

        # INTERNAL Получаем заказы клиента из базы баланса
        orders = db.get_order_by_client(client_id)
        days_order = orders[0]
        steps.CommonSteps.wait_and_get_notification(1, days_order['id'], 1)

        # INTERNAL Если используется тариф с оплатой по дням (Тарифы маркета). Производится расчет денег с оглядкой на это.
        if hasattr(scenario, 'expected_days'):
            expected_money = apikeys_utils.rounded_delta_billing(31, 0, scenario.expected_days, tariff.price)
        else:
            expected_money = money
        # INTERNAL Проверка на схождение остаткнов
        wait(apikeys_steps.get_money_from_shipment,
             mtch.equal_to(expected_money), timeout=W.time, sleep_time=W.s_time)(client_id, link_id)
        # INTERNAL Проверка на состояние связки согласно сценарию активна/не активна
        wait(apikeys_steps.check_link_activation,
             mtch.equal_to(scenario.active_after_scenario), timeout=W.time, sleep_time=W.s_time)(token, key_for_check)

        #todo Понять как сочетать удаление задач из таскера и проверку лимит чекера
        # if hasattr(scenario, 'over_limit') and scenario.over_limit:
        #     utils.check_that(apikeys_steps.check_limit_checker_next_dt(link_id), is_(True),
        #                      u'Проверяем дату следующего запуска limit_checker ')

        ls_paysys_id = db.BalanceBO().execute('select paysys_id from t_invoice where client_id={}'.format(client_id))[
            -1]

        utils.check_that(ls_paysys_id['paysys_id'], is_(person_type.paysys_ls), u'Проверяем paysys_id ЛС')

        # INTERNAL. Закрываем месяц для генерации актов по счету.
        if hasattr(scenario, 'close_month') and scenario.close_month:
            apikeys_steps.close_month(client_id, expected_money, orders, scenario.base_dt + shift(days=70),
                                      nds=person_type.nds)
        return oper_uid, project_id, service_id, person_id, client_id, token, key_for_check, link_id

    @staticmethod
    def not_enough_money(scenario, db_connection, service_cc, tariff, person_type):
        if hasattr(scenario, 'stats'):
            keys_count = len({x.get('key') for x in scenario.stats}) if scenario.stats[0].get('key') else 1
        else:
            keys_count = 1
        # COMPLEX. Генерируем пары ключ-сервис. Создаем плательщика, клиента.
        oper_uid, login, token, service_id, keys, client_id, person_id = \
            apikeys_steps.prepare_several_keys(db_connection, service_cc, keys_count, keys_count)

        tariff_cc = scenario.tariff.replace('apikeys_', '')
        # COMPLEX. Подключаем тариф и плательщика к связке
        project_id, link_id = apikeys_steps.prepare_client_data_for_prepayment(keys.linked[0], oper_uid, service_id,
                                                                               person_id, tariff_cc,
                                                                               on_date=scenario.base_dt,
                                                                               db_connection=db_connection)
        key_for_check = keys.linked[0]
        money = apikeys_utils.get_deposit_money(scenario, tariff)
        # COMPLEX. Выставляем и оплачиваем счет в балансе напрямую
        apikeys_steps.deposit_money(oper_uid, project_id, service_id, person_id, client_id, token, key_for_check, money,
                                    person_type.paysys_invoice)

        # для проверки Таскера нужно закоментировать этот вызов контрактора
        apikeys_api.TEST().run_tarifficator(link_id, scenario.base_dt)
        # todo remove second run_tarifficator after fix APIKEYS-567
        apikeys_api.TEST().run_tarifficator(link_id, scenario.base_dt)

        # COMPLEX. Проверяем, что связка заблокировалась
        wait(apikeys_steps.check_link_activation, is_(False), timeout=W.time, sleep_time=W.s_time)(token, key_for_check)
        wait(apikeys_steps.check_key_activation, is_(False), timeout=W.time, sleep_time=W.s_time)(token, key_for_check)

    @staticmethod
    def two_deposit(scenario, db_connection, service_cc, tariff, person_type):
        first_scenario = utils.aDict(scenario.first_paid)
        first_scenario.tariff = scenario.tariff

        # COMPLEX Запускаем базовый предоплатный сценарий для первого тарифа
        oper_uid, project_id, service_id, person_id, client_id, token, key_for_check, link_id = \
            Prepayment.basic(first_scenario, db_connection, service_cc, tariff, person_type)

        second_scenario = utils.aDict(scenario.second_paid)
        second_scenario.tariff = scenario.tariff

        money = apikeys_utils.get_deposit_money(second_scenario, tariff)

        # COMPLEX Оплачиваем сумму второго депозита
        apikeys_steps.deposit_money(oper_uid, project_id, service_id, person_id, client_id, token, key_for_check, money,
                                    person_type.paysys_invoice, check_link_and_key=False,
                                    on_date=second_scenario.base_dt)

        # для проверки Таскера нужно закоментировать этот вызов контрактора
        apikeys_api.TEST().run_tarifficator(link_id, second_scenario.base_dt)
        # todo remove second run_tarifficator after fix APIKEYS-567
        apikeys_api.TEST().run_tarifficator(link_id, second_scenario.base_dt)

        wait(apikeys_steps.check_link_activation, is_(True), timeout=W.time, sleep_time=W.s_time)(token, key_for_check)

        for day in second_scenario.tarifficator_days:
            apikeys_api.TEST().run_tarifficator(link_id, second_scenario.base_dt + shift(days=day))

            # todo remove second run_tarifficator after fix APIKEYS-567
            apikeys_api.TEST().run_tarifficator(link_id, second_scenario.base_dt + shift(days=day))
        # BALANCE_BACKEND получаем список заказов клиента
        orders = db.get_order_by_client(client_id)
        steps.CommonSteps.wait_and_get_notification(1, orders[0]['id'], 1)
        expected_money = apikeys_utils.rounded_delta_billing(31, 0, second_scenario.expected_days, tariff.price)
        # INTERNAL  проверяем верно ли открутились средства
        wait(apikeys_steps.get_money_from_shipment, mtch.equal_to(expected_money), timeout=W.time, sleep_time=W.s_time)(
            client_id, link_id, second_scenario.base_dt + shift(days=second_scenario.tarifficator_days[-1]))
        # INTERNAL  проверяем активность связки
        wait(apikeys_steps.check_link_activation,
             mtch.equal_to(second_scenario.active_after_scenario), timeout=W.time, sleep_time=W.s_time)(token,
                                                                                                        key_for_check)

    @staticmethod
    def change_tariff(scenario, db_connection, person_type):
        first_scenario = utils.aDict(scenario.first_tariff)
        first_tariff = first_scenario.tariff
        service_cc = first_scenario.tariff.service_id
        first_scenario.tariff = first_scenario.tariff.name

        # COMPLEX Запускаем базовый предоплатный сценарий для первого тарифа
        oper_uid, project_id, service_id, person_id, client_id, token, key_for_check, link_id = \
            Prepayment.basic(first_scenario, db_connection, service_cc, first_tariff, person_type)

        second_scenario = utils.aDict(scenario.second_tariff)
        second_tariff = second_scenario.tariff
        second_scenario.tariff = second_scenario.tariff.name
        new_tariff_cc = second_scenario.tariff.replace('apikeys_', '')
        # APIKEYS_CLIENT Меняем тариф
        apikeys_api.BO().schedule_tariff_changing(ADMIN, key_for_check, service_id, new_tariff_cc)
        # INTERNAL Сдвигаем дату смены тарифа на сегодя
        apikeys_steps.change_scheduled_tariff_date(project_id, second_scenario.base_dt)
        new_link_id = apikeys_steps.get_link_by_key(key_for_check)[0]['link_id']

        wait(apikeys_steps.get_tariff_from_project_service_link, matcher=mtch.equal_to(new_tariff_cc),
             timeout=W.time,
             sleep_time=W.s_time)(new_link_id, project_id, second_scenario.base_dt)

        # для проверки Таскера нужно закоментировать этот вызов тарификатора
        apikeys_api.TEST().run_tarifficator(new_link_id, second_scenario.base_dt)
        # todo remove second run_tarifficator after fix APIKEYS-567
        apikeys_api.TEST().run_tarifficator(new_link_id, second_scenario.base_dt)

        wait(apikeys_steps.check_link_activation, is_(True), timeout=W.time, sleep_time=W.s_time)(token, key_for_check)

        for day in second_scenario.tarifficator_days:
            apikeys_api.TEST().run_tarifficator(new_link_id, second_scenario.base_dt + shift(days=day))

            # todo remove second run_tarifficator after fix APIKEYS-567
            apikeys_api.TEST().run_tarifficator(new_link_id, second_scenario.base_dt + shift(days=day, minutes=1))

        orders = db.get_order_by_client(client_id)
        steps.CommonSteps.wait_and_get_notification(1, orders[0]['id'], 1)
        days_order = orders[0]
        if second_scenario.expected_days:
            utils.check_that(len(orders), is_(2), u'Проверяем наличие второго заказа')
            expected_money = apikeys_utils.rounded_delta_billing(31, 0, second_scenario.expected_days,
                                                                 second_tariff.price) \
                             + apikeys_utils.rounded_delta_billing(31, 0, first_scenario.expected_days,
                                                                   first_tariff.price)
            wait(apikeys_steps.get_money_from_shipment,
                 mtch.equal_to(expected_money), timeout=W.time, sleep_time=W.s_time)(client_id, link_id)
        else:
            utils.check_that(len(orders), is_(1), u'Проверяем отсутсвие второго заказа')

        wait(apikeys_steps.check_link_activation,
             mtch.equal_to(second_scenario.active_after_scenario), timeout=W.time, sleep_time=W.s_time)(token,
                                                                                                        key_for_check)

    @staticmethod
    def pull_lazy_client(scenario, free_passport, service_cc):
        oper_uid, login, token, service_id, client_id, person_id = apikeys_steps.prepare_lazy_client(free_passport,
                                                                                                     service_cc)

        apikeys_steps.create_future_postpay_contract(scenario, service_cc, client_id, person_id)

        apikeys_steps.move_dt_for_pull_clients_task()

        # TODO: проверка
        utils.check_that(apikeys_api.TEST.mongo_find('task', {"_cls": "Task.PullClientsTask"})[0]['last_status'],
                         is_('OK'), u'Проверяем, что pull_clients_task отработал успешно')

    @staticmethod
    def overlimit(scenario, db_connection, service_cc, tariff, person_type):
        oper_uid, project_id, service_id, person_id, client_id, token, key_for_check, link_id = \
            Prepayment.basic(scenario, db_connection, service_cc, tariff, person_type)

        money = apikeys_utils.get_deposit_money(scenario, tariff) * scenario.extra_money_multiplier
        apikeys_steps.deposit_money(oper_uid, project_id, service_id, person_id, client_id, token, key_for_check, money,
                                    person_type.paysys_invoice, check_link_and_key=False)

    @staticmethod
    def contract(scenario, db_connection, service_cc, tariff, person_type):
        keys_count = len({x.get('key') for x in scenario.stats}) if len({x.get('key') for x in scenario.stats}) else 1
        oper_uid, login, token, service_id, keys, client_id, person_id = apikeys_steps.prepare_several_keys(
            db_connection, service_cc, keys_count, keys_count, person_type.type)
        contract_id = apikeys_steps.create_active_postpay_contract(scenario,
                                                                   service_cc,
                                                                   client_id,
                                                                   person_id,
                                                                   scenario.signed_date)

        apikeys_api.BO().get_client_from_balance(ADMIN, oper_uid)
        apikeys_api.TEST().run_user_contractor(oper_uid)
        key_for_check = keys.linked[0]
        project_id = apikeys_steps.get_project_id_by_key(key_for_check)
        link_id = apikeys_steps.get_link_by_key(key_for_check)[0]['link_id']
        apikeys_api.TEST().run_tarifficator(link_id, scenario.base_dt)
        wait(apikeys_steps.check_link_activation, is_(False), timeout=W.time, sleep_time=W.s_time)(token, key_for_check)
        apikeys_steps.deposit_money(oper_uid, project_id, service_id, person_id, client_id, token, key_for_check,
                                    tariff.month_price, person_type.paysys_invoice, on_date=scenario.base_dt,
                                    contract_id=contract_id)
        apikeys_api.TEST().run_tarifficator(link_id, scenario.base_dt)
        wait(apikeys_steps.check_link_activation, is_(True), timeout=W.time, sleep_time=W.s_time)(token, key_for_check)
        wait(apikeys_steps.check_key_activation, is_(True), timeout=W.time, sleep_time=W.s_time)(token, key_for_check)
        dates = sorted([row.get('dt') for row in scenario.stats])
        first_date = dates[0] - shift(hours=1)
        last_date = dates[-1] + shift(hours=1)
        apikeys_steps.change_and_check_tarifficator_activated_date(link_id, oper_uid, project_id, first_date)
        scenario = apikeys_steps.add_counters_to_scenario(scenario, token, keys.linked)
        apikeys_steps.process_every_stat(scenario, oper_uid, link_id,db_connection)
        apikeys_api.TEST.run_tarifficator(link_id, last_date)
        orders = db.get_order_by_client(client_id)
        wait(apikeys_steps.get_money_from_shipment,
             mtch.equal_to(scenario.expected), timeout=W.time, sleep_time=W.s_time)(client_id, link_id)
        wait(apikeys_steps.check_link_activation,
             mtch.equal_to(scenario.active_after_scenario), timeout=W.time, sleep_time=W.s_time)(token, key_for_check)
        if hasattr(scenario, 'close_month') and scenario.close_month:
            apikeys_steps.close_month(client_id, scenario.expected, orders, last_date, contract_id=contract_id,
                                      nds=True)

    @staticmethod
    def postpayment_with_prepayment_month(scenario, free_passport, service_cc, tariff, person_type):
        keys_count = len({x.get('key') for x in scenario.stats}) if scenario.stats[0].get('key') else 1

        scenario.stats = plain.completions_shift_limit(scenario.stats, tariff.day_limit, tariff.counters)

        oper_uid, login, token, service_id, keys, client_id, person_id = apikeys_steps.prepare_several_keys(
            free_passport,
            service_cc,
            keys_count,
            keys_count,
            person_type=person_type.type)

        contract_id, project_id = apikeys_steps.prepare_and_move_contract(client_id, keys.linked[0], oper_uid,
                                                                          person_id,
                                                                          scenario, service_cc, service_id)

        link_id = apikeys_steps.get_link_by_key(keys.linked[0])[0]['link_id']
        first_date = sorted([row.get('dt') for row in scenario.stats])[-1] - shift(hours=1)
        apikeys_steps.change_and_check_tarifficator_activated_date(link_id, oper_uid, project_id, first_date)

        scenario = apikeys_steps.add_counters_to_scenario(scenario, token, keys.linked)
        apikeys_steps.process_every_stat(scenario, oper_uid, link_id,None)

        orders = db.get_order_by_client(client_id)
        print '[DEBUG] ...... Orders: {}'.format(orders)
        orders.sort(key=lambda x: x['service_order_id'])
        days_order = orders[0]
        shipments = db.get_shipments_by_service_order_id({'service_order_id': days_order['service_order_id'],
                                                          'service_id': days_order['service_id']})

        api.test_balance().Campaigns({'service_id': days_order['service_id'],
                                      'service_order_id': days_order['service_order_id'],
                                      'use_current_shipment': True})

        utils.check_that(D(shipments[0]['days']), (mtch.equal_to(D(tariff.year_price))),
                         u'Выполняем проверку суммы абонплаты')

        if scenario.over_limit:
            expected_money = D(plain.calculate_over_limit_expected(scenario.stats, tariff.day_limit,
                                                                   tariff.price_over_limit_per_1000))
            units_order = orders[1]
            shipments = db.get_shipments_by_service_order_id({'service_order_id': units_order['service_order_id'],
                                                              'service_id': units_order['service_id']})

            utils.check_that(D(shipments[0]['money']), (mtch.equal_to(D(expected_money))),
                             u'Выполняем проверку суммы перекруток')

        else:
            utils.check_that(len(orders), (mtch.equal_to(1)),
                             u'Выполняем проверку, что нет заказа по перекруткам')
            expected_money = 0

        expected_money += tariff.year_price
        if hasattr(scenario, 'close_month') and scenario.close_month:
            apikeys_steps.close_month(client_id, expected_money, orders, scenario.base_dt + shift(days=70))
