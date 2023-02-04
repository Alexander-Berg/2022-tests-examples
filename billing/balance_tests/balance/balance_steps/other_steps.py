# coding=utf-8
__author__ = 'igogor'

from contextlib import contextmanager

import json
import datetime

import acts_steps
import balance.balance_api as api
import balance.balance_db as db
import btestlib.environments as env
import btestlib.passport_steps as passport_steps
import btestlib.reporter as reporter
import btestlib.shared as shared
import btestlib.utils as utils
from btestlib import utils_tvm
import campaigh_steps
import client_steps
import distribution_steps
import invoice_steps
import taxi_steps
from btestlib.data import defaults
from simpleapi.common.utils import call_http

try:
    from typing import Callable, ContextManager, Union
except ImportError:
    pass

log_align = 30
# log = reporter.logger()

# еще дефолтные даты есть в btestlib.data.defaults.Date
to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
A_DAY_AFTER_TOMORROW = NOW + datetime.timedelta(days=2)


# ---------------------------------------------------------------------------------------------------------------------

# TODO: refactor it!
class TransferSteps(object):
    @staticmethod
    def create_operation(passport_uid=defaults.PASSPORT_UID):
        return api.medium().CreateOperation(passport_uid)


# ---------------------------------------------------------------------------------------------------------------------

class PaymentTermSteps(object):
    @staticmethod
    def payment_term_with_holidays(payment_term, dt_from):
        dt_from = utils.Date.nullify_time_of_date(date=dt_from)
        payment_term_with_holidays = db.balance().execute('''SELECT anon_1
        FROM (SELECT anon_1, ROWNUM AS ora_rn
        FROM (SELECT dt - :dt_from AS anon_1
        FROM mv_working_calendar
        WHERE mv_working_calendar.dt >= :dt_from
          AND (mv_working_calendar.calendar_day = 1
                 OR mv_working_calendar.five_day = 1)
          AND mv_working_calendar.region_id = 225 ORDER BY mv_working_calendar.dt)
        WHERE ROWNUM <= :payment_term+1)
        WHERE ora_rn > :payment_term''', {'payment_term': payment_term, 'dt_from': dt_from})[0]['anon_1']
        return dt_from+datetime.timedelta(days=payment_term_with_holidays)


# ---------------------------------------------------------------------------------------------------------------------
class CloseMonth(object):
    STATE_ID_NAME_MAPPER = {0: 'new',
                            1: 'open',
                            2: 'resolved',
                            3: 'stalled'
                            }

    @staticmethod
    def update_limits(dt, force_value, client_ids):
        with reporter.step(u'Запускаем пересчет кредитного лимита.'):
            api.test_balance().UpdateLimits(dt, force_value, client_ids)

    @staticmethod
    def close_firms(dt):
        api.test_balance().CloseFirms(dt)

    @staticmethod
    def get_mnclose_status(service_token, task_name):
        result = api.medium().GetMncloseStatus(service_token, task_name)
        return result

    @staticmethod
    def resolve_mnclose_status_for_service(service_token, task_name):
        result = api.medium().ResolveMncloseTask(service_token, task_name)
        return result

    @staticmethod
    def resolve_mnclose_status(task_name):
        result = api.test_balance().ResolveMncloseTask(task_name)
        return result

    @staticmethod
    def get_state(task_name):
        query = '''select status from bo.T_NIRVANA_MNCLOSE_SYNC
        where task_id = :task_name and dt = trunc(sysdate, 'MONTH')'''
        query_params = {'task_name': task_name}
        last_state_change = db.balance().execute(query, query_params)
        if len(last_state_change) == 0:
            return None
        else:
            return last_state_change[0]

    @staticmethod
    def set_state_force(task_name, state):
        if CloseMonth.get_state(task_name):
            query = '''update bo.T_NIRVANA_MNCLOSE_SYNC set status = :state
            where task_id = :task_name and dt = trunc(sysdate, 'MONTH')'''
        else:
            query = '''insert into BO.T_NIRVANA_MNCLOSE_SYNC (TASK_ID, DT, STATUS)
            values (:task_name, trunc(sysdate, 'MONTH'), :state)'''
        query_params = {'task_name': task_name, 'state': state}
        db.balance().execute(query, query_params)

    @staticmethod
    def resolve_task(task_name):
        CloseMonth.set_state_force(task_name, 'resolved')

# ---------------------------------------------------------------------------------------------------------------------

class DiscountSteps(object):
    @staticmethod
    def prepare_budget_by_payments(budget_owner, person_id, contract_id, paysys_id, budget_list):
        for product, completions, dt in budget_list:
            dt_1st = dt.replace(day=1)
            tmp_client_id = client_steps.ClientSteps.create({'IS_AGENCY': 0})
            campaigns_list = [
                {'client_id': tmp_client_id, 'service_id': product.service_id, 'product_id': product.id,
                 'qty': completions,
                 'begin_dt': dt_1st}
            ]
            invoice_id, _, _, orders_list = invoice_steps.InvoiceSteps.create_force_invoice(client_id=tmp_client_id,
                                                                                            person_id=person_id,
                                                                                            campaigns_list=campaigns_list,
                                                                                            paysys_id=paysys_id,
                                                                                            invoice_dt=dt_1st,
                                                                                            agency_id=budget_owner,
                                                                                            # credit=p.get('is_fpa_budget', 0),
                                                                                            credit=0,
                                                                                            contract_id=contract_id,
                                                                                            overdraft=0,
                                                                                            manager_uid=None)
            invoice_steps.InvoiceSteps.pay(invoice_id)
            campaigh_steps.CampaignsSteps.do_campaigns(product.service_id, orders_list[0]['ServiceOrderID'],
                                                       {product.shipment_type: completions}, 0, dt_1st)

    @staticmethod
    def prepare_budget_by_acts(budget_owner, person_id, contract_id, paysys_id, budget_list):
        DiscountSteps.prepare_budget_by_payments(budget_owner, person_id, contract_id, paysys_id, budget_list)
        for product, completions, dt in budget_list:
            acts_steps.ActsSteps.generate(budget_owner, 1, dt)

    @staticmethod
    def get_client_discounts_all(params):
        with reporter.step(u'Проверяем ответ метода GetClientDiscountsAll:'):
            result = api.medium().GetClientDiscountsAll(params)
        return result

    @staticmethod
    def estimate_discount(params, orders_list):
        with reporter.step(u'Проверяем ответ метода EstimateDiscount:'):
            result = api.medium().EstimateDiscount(params, orders_list)
        return result


# ---------------------------------------------------------------------------------------------------------------------


class ConfigSteps(object):
    _PLUS_CONFIG_ITEM = 'PLUS_2_0_CONFIGURATION'
    _PLUS_CONFIG_SCHEME_VERSION_ITEM = 'PLUS_2_0_CONFIGURATION_SCHEME_VERSION'
    _PLUS_CONFIG_LEGACY_SCHEME = 'LEGACY01'
    _PLUS_PARTS = 'parts'
    _PLUS_PART_KEY = 'part_key'
    _PLUS_PART_CONFIG = 'config'

    @staticmethod
    def set_config_value(item, value_dt=None, value_num=None, value_str=None, value_json=None):
        config_item = db.balance().execute('SELECT * FROM t_config WHERE item = :item', {'item': item})[0]
        if not config_item:
            raise Exception("{0} is not in t_config" + str(item))
        config_default = {'value_json': value_json,
                          'value_dt': value_dt,
                          'value_num': value_num,
                          'value_str': value_str}
        config_default_for_update = utils.remove_empty(config_default)
        query = ('''update T_CONFIG set {0} where item = :item'''.format(
            (', '.join('{0}=\'{1}\''.format(k, v) for k, v in config_default_for_update.items())), ))
        db.balance().execute(query, {'item': item})

    @staticmethod
    def get_plus_scheme_version():
        with reporter.step(u'Получаем версию схемы конфигурации Плюса'):
            rows = db.balance().execute("select value_str from bo.t_config where item = :item",
                                        {'item': ConfigSteps._PLUS_CONFIG_SCHEME_VERSION_ITEM})
        if not rows:
            return 'LAST'
        return rows[0]['value_str']

    @staticmethod
    def get_plus_whole_config():
        """Не используется,
         смотри историю в RCS для примера одновременной поддержки сильно различающихся форматов конфигурации Плюса"""
        with reporter.step(u'Получаем полную конфигурацию Плюса'):
            rows = db.balance().execute("select value_json from bo.t_config where item = :item",
                                        {'item': ConfigSteps._PLUS_CONFIG_ITEM})
        if not rows:
            raise ValueError('There is no {} in t_config'.format(ConfigSteps._PLUS_CONFIG_ITEM))
        whole_config = json.loads(rows[0]['value_json'])
        return whole_config

    @staticmethod
    def set_plus_whole_config(config):
        with reporter.step(u'Устанавливаем полную конфигурацию Плюса: {}'.format(config)):
            ConfigSteps.set_config_value(item=ConfigSteps._PLUS_CONFIG_ITEM, value_json=json.dumps(config))

    @staticmethod
    def get_plus_part_config(plus_part_key):
        with reporter.step(u'Получаем конфигурацию части Плюса {}={}'.format(ConfigSteps._PLUS_PART_KEY, plus_part_key)):
            whole_config = ConfigSteps.get_plus_whole_config()
        configs = [p_p_c[ConfigSteps._PLUS_PART_CONFIG] for p_p_c in whole_config[ConfigSteps._PLUS_PARTS]
                   if p_p_c[ConfigSteps._PLUS_PART_KEY] == plus_part_key]
        if len(configs) == 0:
            return None
        if len(configs) > 1:
            raise ValueError('Multiple configs for {}={}'.format(ConfigSteps._PLUS_PART_KEY, plus_part_key))
        return configs[0]

    @staticmethod
    def set_plus_part_config(config, plus_part_key):
        with reporter.step(
                u'Устанавливаем конфигурацию части Плюса {}={}: {}'.format(ConfigSteps._PLUS_PART_KEY, plus_part_key, config)):
            whole_config = ConfigSteps.get_plus_whole_config()
            other_ = [p_p_c for p_p_c in whole_config[ConfigSteps._PLUS_PARTS] if
                      p_p_c[ConfigSteps._PLUS_PART_KEY] != plus_part_key]
            other_.append({ConfigSteps._PLUS_PART_KEY: plus_part_key, ConfigSteps._PLUS_PART_CONFIG: config})
            whole_config[ConfigSteps._PLUS_PARTS] = other_
            ConfigSteps.set_plus_whole_config(whole_config)

    @staticmethod
    def temporary_changer(item, db_column):  # type: (str, str) -> Callable
        def decorator(changer):  # type: (Callable) -> ContextManager
            @contextmanager
            def manager(*args, **kwargs):
                select_query = 'select {} from t_config where item=:item'.format(db_column)
                update_query = 'update t_config set {}=:value where item=:item'.format(db_column)

                params = {'item': item}
                with reporter.step(u'Получаем значение конфига {}'.format(item)):
                    initial_config_value = db.balance().execute(select_query, params)[0][db_column]
                changed_config_value = changer(initial_config_value, *args, **kwargs)

                params['value'] = changed_config_value
                with reporter.step(u'Временно изменяем конфиг {}'.format(item)):
                    db.balance().execute(update_query, params)

                yield

                params['value'] = initial_config_value
                with reporter.step(u'Восстанавливаем исходное состояние конфига {}'.format(item)):
                    db.balance().execute(update_query, params)

            return manager

        return decorator


class PermissionsSteps(object):
    @staticmethod
    def get_all_permission_ids():
        with reporter.step(u'Получаем список всех прав из t_permission'):
            query = 'SELECT perm FROM t_permission'
            perm_ids = db.balance().execute(query)
            return [line.get('perm') for line in perm_ids]

    @staticmethod
    def get_permission_codes(perm_ids):
        with reporter.step(u'Получаем названия прав из t_permission'):
            query = 'select perm, code from t_permission where perm in ({perms})'.format(
                perms=','.join(map(str, perm_ids)))
            res = db.balance().execute(query)
            return dict([(line['perm'], line['code']) for line in res])

    @staticmethod
    def find_or_create_role_with_permissions_strict(perm_ids):
        test_role_name_pattern = 'Autotest role'

        def find_role_with_permissions_strict(perm_ids):
            with reporter.step(u'Ищем тестовую роль с необходимыми правами'):
                query = '''SELECT role_id, LISTAGG(perm, ',') WITHIN GROUP (ORDER BY perm) AS perms
                        FROM t_role
                        where role_id in (select id from T_ROLE_NAME where NAME LIKE '{role_name_ptn}%')
                        GROUP BY role_id'''.format(
                    role_name_ptn=test_role_name_pattern)
                test_roles = db.balance().execute(query, descr='Считываем все тестовые роли из БД')

                sorted_perms_str = ','.join(map(str, sorted(perm_ids)))
                roles_with_perms = [line['role_id'] for line in test_roles if line['perms'] == sorted_perms_str]
                return roles_with_perms[0] if roles_with_perms else None

        def create_role_with_permissions(perm_ids):
            with reporter.step(u'Создаем тестовую роль с необходимыми правами'):
                role_id = db.balance().sequence_nextval('S_ROLE_ID')
                role_name = '{role_name_ptn} {role_id}'.format(role_name_ptn=test_role_name_pattern, role_id=role_id)
                query = 'INSERT INTO t_role_name (id, name) VALUES (:role_id, :role_name)'

                db.balance().execute(query, {'role_id': role_id, 'role_name': role_name},
                                     descr='Создаем роль')
                add_permissions_to_role(role_id, perm_ids)
                return role_id

        def add_permissions_to_role(role_id, perm_ids):
            with reporter.step(u'Добавляем права в роль'):
                query = 'INSERT INTO t_role (perm, role_id) VALUES (:perm_id, :role_id)'
                for perm_id in perm_ids:
                    db.balance().execute(query, {'perm_id': perm_id, 'role_id': role_id})

        with reporter.step(u'Ищем/создаем тестовую роль, которая содержит только необходимые права'):
            reporter.attach(u'Необходимые права',
                            utils.Presenter.pretty(PermissionsSteps.get_permission_codes(perm_ids)))
            role_id = find_role_with_permissions_strict(perm_ids)
            return role_id if role_id is not None else create_role_with_permissions(perm_ids)


class UserSteps(object):
    @staticmethod
    def is_known_user(user):
        with reporter.step(u'Проверяем наличие логина {login} в t_account'.format(login=user.login)):
            query = 'select count(1) as qty from t_account where passport_id = {uid}'.format(uid=user.uid)
            return db.balance().execute(query, single_row=True)['qty'] != 0

    @staticmethod
    def set_role(user, role_id):
        with reporter.step(
                u'Устанавливаем роль {role_id} для логина {login}'.format(role_id=role_id, login=user.login)):
            UserSteps.add_user_if_needed(user)

            query = 'insert into t_role_user (passport_id, role_id) values ({uid}, {role_id})'.format(
                uid=user.uid, role_id=role_id)
            db.balance().execute(query)

    @staticmethod
    def add_user_if_needed(user):
        with reporter.step(u'Создаем запись о логине {login} в t_account, если такой еще нет'.format(login=user.login)):
            if not UserSteps.is_known_user(user):
                api.medium().GetPassportByUid(defaults.PASSPORT_UID, user.uid)

    @staticmethod
    def link_user_and_client(user, client_id):
        with reporter.step(
                u'Связываем клиента (id: {client_id}) с логином {login}'.format(client_id=client_id, login=user.login)):
            UserSteps.add_user_if_needed(user)
            client_steps.ClientSteps.link(client_id, user.login)

    @staticmethod
    def set_role_with_permissions_strict(user, perm_ids):
        with reporter.step(u'Устанавливаем для логина {login} тестовую роль, '
                           u'которая содержит только необходимые права'.format(login=user.login)):
            role_id = PermissionsSteps.find_or_create_role_with_permissions_strict(perm_ids)
            user_roles = UserSteps.get_roles(user)
            if len(user_roles) == 1 and user_roles[0] == role_id:
                reporter.attach(u'У логина {login} уже установлена нужная роль {role_id}'.format(login=user.login,
                                                                                                 role_id=role_id))
            else:
                if len(user_roles) > 0:
                    UserSteps.clear_roles(user)
                UserSteps.set_role(user, role_id)

    @staticmethod
    def get_roles(user):
        with reporter.step(u'Получаем роли логина {login}'.format(login=user.login)):
            query = 'select role_id from t_role_user where passport_id = {uid}'.format(uid=user.uid)
            role_ids = db.balance().execute(query)
            return [line['role_id'] for line in role_ids]

    @staticmethod
    def clear_roles(user):
        with reporter.step(u'Отвязываем все роли от логина {login}'.format(login=user.login)):
            query = 'delete t_role_user where passport_id = {uid}'.format(uid=user.uid)
            db.balance().execute(query)

    @staticmethod
    def link_accountant_and_client(user, client_id):
        with reporter.step(u'Связываем клиента (id: {client_id}) с бухгалтерским логином {login}'.format(client_id=client_id, login=user.login)):
            db.balance().execute("DELETE FROM t_role_client_user WHERE passport_id = :passport_id", {'passport_id': user.uid})
            db.balance().execute("INSERT INTO t_role_client_user (ID,PASSPORT_ID,CLIENT_ID,ROLE_ID,CREATE_DT,UPDATE_DT) values (s_role_client_user_id.nextval,:passport_id,:client_id,100,:dt,:dt)",
                    {'passport_id': user.uid, 'client_id': client_id, 'dt': datetime.datetime.now()})

    @staticmethod
    def get_passport_id_by_login(login):
        return db.balance().execute("SELECT passport_id FROM t_passport WHERE login = :login",
                            {'login': login})[0]['passport_id']


class PassportSteps(object):
    @staticmethod
    def get_passport_by_uid(uid, relations=None):
        with reporter.step(u"Зовем GetPassportByUid"):
            if relations is not None:
                return api.medium().GetPassportByUid(defaults.PASSPORT_UID, uid, relations)
            else:
                return api.medium().GetPassportByUid(defaults.PASSPORT_UID, uid)

    @staticmethod
    def get_passport_by_login(login, relations=None):
        with reporter.step(u"Зовем GetPassportByLogin"):
            if relations is not None:
                return api.medium().GetPassportByLogin(defaults.PASSPORT_UID, login, relations)
            else:
                return api.medium().GetPassportByLogin(defaults.PASSPORT_UID, login)

    @staticmethod
    def delete_all_roles_from_passport(uid):
        db.balance().execute('''DELETE  FROM t_role_user WHERE passport_id = :passport_id''', {'passport_id': uid})


class TrustApiSteps(object):
    @staticmethod
    def get_request_payment_methods(request_id, passport_id, contract_id=None, person_id=None):
        return api.medium().GetRequestPaymentMethods(
            {
                'OperatorUid': passport_id,
                'RequestID': request_id,
                'PersonID': person_id,
                'ContractId': contract_id
            })

    @staticmethod
    def get_card_binding_url(service_id, currency, passport_id, return_path=None, payload=None,
                             notification_url=None):
        return api.medium().GetCardBindingURL(passport_id, {'ServiceID': service_id,
                                                                  'Currency': currency,
                                                                  'ReturnPath': return_path,
                                                                  'NotificationURL': notification_url,
                                                                  'Payload': payload})

    @staticmethod
    def check_binding(passport_id, token, service_id):
        return api.medium().CheckBinding(passport_id, {'ServiceID': service_id, 'PurchaseToken': token})

    @staticmethod
    def get_bound_payment_methods(service_id, passport_id):
        return api.medium().GetBoundPaymentMethods(passport_id, service_id)

    @staticmethod
    def pay_request(passport_id, request_id, payment_method_id, currency_code, person_id=None, contract_id=None,
                    legal_entity=None, resident=None, region_id=None, notification_url=None, payload=None,
                    redirect_url=None, receipt_email=None):
        params = {'RequestID': request_id,
                  'PaymentMethodID': payment_method_id,
                  'Currency': currency_code,
                  'PersonID': person_id,
                  'ContractID': contract_id,
                  'NotificationURL': notification_url,
                  'Payload': payload,
                  'RedirectURL': redirect_url,
                  'ReceiptEmail': receipt_email}
        if legal_entity or resident or region_id:
            params.update({'LegalEntity': legal_entity,
                           'Resident': resident,
                           'RegionID': region_id})
            del params['PersonID']

        return api.medium().PayRequest(passport_id, params)

    @staticmethod
    def check_request_payment(passport_id, service_id, request_id=None, transaction_id=None):
        return api.medium().CheckRequestPayment(passport_id, {'RequestID': request_id,
                                                                    'ServiceID': service_id,
                                                                    'TransactionID': transaction_id})


class BadDebtSteps(object):
    @staticmethod
    def make_bad_debt(invoice_id, our_fault):
        session = passport_steps.auth_session()
        make_bad_debt_url = '{base_url}/set-bad-debt.xml'.format(base_url=env.balance_env().balance_ai)
        params = {'invoice_id': invoice_id, 'commentary': 'test'}
        if our_fault:
            params.update({'our-fault': our_fault})
        headers = {'X-Requested-With': 'XMLHttpRequest'}
        utils.call_http(session, make_bad_debt_url, params, headers, method='GET')
        return invoice_id

    @staticmethod
    def make_bad_debt_thru_db(act_id, our_fault=False, hidden=False, comment='XXXDEBT666'):
        params = dict(act_id=act_id, hidden=int(bool(hidden)), comment=comment, our_fault=int(bool(our_fault)),
                      uid=defaults.PASSPORT_UID)
        db.balance().execute('''INSERT INTO t_bad_debt_act(act_id, passport_id, dt, hidden, commentary, our_fault)
values (:act_id, :uid, sysdate, :hidden, :comment, :our_fault)''', params)

    @staticmethod
    def make_our_fault(act_id):
        db.balance().execute('UPDATE t_bad_debt_act SET our_fault = 1 WHERE act_id = :act_id', {'act_id': act_id})

    @staticmethod
    def make_not_our_fault(act_id):
        db.balance().execute('UPDATE t_bad_debt_act SET our_fault = 0 WHERE act_id = :act_id', {'act_id': act_id})

    @staticmethod
    def make_bad_debt_hidden(act_id):
        db.balance().execute('UPDATE t_bad_debt_act SET hidden = 1 WHERE act_id = :act_id', {'act_id': act_id})

    @staticmethod
    def make_bad_debt_unhidden(act_id):
        db.balance().execute('UPDATE t_bad_debt_act SET hidden = 0 WHERE act_id = :act_id', {'act_id': act_id})

    @staticmethod
    def is_bad_debt(act_id):
        with reporter.step(u'Проверяем признан ли акт (id={id}) плохим долгом'.format(id=act_id)):
            query = 'select count(1) as qty from T_BAD_DEBT_ACT where ACT_ID = {id} and hidden = 0'.format(id=act_id)
            return db.balance().execute(query, single_row=True)['qty'] == 1


class SharedBlocks(object):
    UPDATE_DISTR_VIEWS = 'update_distr_views'
    REFRESH_TAXI_CONTRACT_MVIEWS = 'refresh_taxi_contract_mviews'
    REFRESH_PARTNER_CONTRACT_PLACE_MVIEWS = 'refresh_partner_contract_place_mviews'
    # REFRESH_MV_CLIENT_DIRECT_BRAND = 'refresh_mv_client_direct_brand'  # removed due to BALANCE-29154
    REFRESH_MV_PARTNER_DSP_CONTRACT = 'refresh_mv_partner_dsp_contract'
    # NO_ACTIONS = 'no_actions'

    TEST1 = 'tst1'

    @staticmethod
    def tst1(shared_data, before):
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.TEST1) as block:
            block.validate()
            reporter.log('loooooooooooooooooooooooooooooooooooooooong action')

    @staticmethod
    def update_distr_views(shared_data, before):
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.UPDATE_DISTR_VIEWS) as block:
            block.validate()

            distribution_steps.DistributionSteps.update_distr_views()

    # TODO: сделать так, чтобы при запуске из веток обновление шло на ts, а в остальных случаях - туда, откуда запустили
    @staticmethod
    def refresh_taxi_contract_mviews(shared_data, before):
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.REFRESH_TAXI_CONTRACT_MVIEWS) as block:
            block.validate()
            with reporter.step(u'Обновляем матвью с договорами такси'):
                db.balance().execute("BEGIN dbms_mview.refresh('BO.MV_PARTNER_TAXI_CONTRACT',method => 'C',atomic_refresh => false); END;")

    @staticmethod
    def refresh_partner_contract_place_mviews(shared_data, before):
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.REFRESH_PARTNER_CONTRACT_PLACE_MVIEWS) as block:
            block.validate()

            db.balance().execute("BEGIN dbms_mview.refresh('BO.MV_PARTNER_CONTRACT_PUTTEE','C'); END;")
            db.balance().execute("BEGIN dbms_mview.refresh('BO.mv_partner_place_owners','C'); END;")

    # @staticmethod
    # def refresh_mv_client_direct_brand(shared_data, before):
    #     with shared.SharedBlock(shared_data=shared_data, before=before,
    #                             block_name=SharedBlocks.REFRESH_MV_CLIENT_DIRECT_BRAND) as block:
    #         block.validate()
    #
    #         db.balance().execute("BEGIN dbms_mview.refresh('BO.mv_client_direct_brand','C'); END;")

    @staticmethod
    def refresh_mv_partner_dsp_contract(shared_data, before):
        with shared.SharedBlock(shared_data=shared_data, before=before,
                                block_name=SharedBlocks.REFRESH_MV_PARTNER_DSP_CONTRACT) as block:
            block.validate()

            db.balance().execute("BEGIN dbms_mview.refresh('BO.MV_PARTNER_DSP_CONTRACT','C'); END;")

            # @staticmethod
            # def no_actions(shared_data, before):
            #     with shared.SharedBlock(shared_data=shared_data, before=before,
            #                             block_name=SharedBlocks.NO_ACTIONS) as block:
            #         block.validate()
            #
            #         # Утилитный блок для тестов, добавляемых в shared_block по причине длительного времени работы


class MediumHttpSteps(object):
    @staticmethod
    def get_payment_batch_details(payment_batch_id):
        with reporter.step(u"Получаем подробности платежей по payment_batch_id: {}".format(payment_batch_id)):
            method_url = '{base_url}/get_payment_batch_details'.format(base_url=env.balance_env().medium_http_url)
            params = {
                'payment_batch_id': payment_batch_id
            }
            headers = utils_tvm.supply_tvm_ticket({})
            csv_data = call_http(method_url, params, method='GET', headers=headers)
            return utils.csv_data_to_dict_list(csv_data, '\t')

    @staticmethod
    def get_payment_headers(service, from_trantime, to_trantime):
        with reporter.step(u"Зовём ручку get_payment_headers"):
            method_url = '{base_url}/get_payment_headers'.format(base_url=env.balance_env().medium_http_url)
            params = {
                'from_trantime': from_trantime,
                'to_trantime': to_trantime,
                'service_id': service.id
            }
            headers = utils_tvm.supply_tvm_ticket({})
            csv_data = call_http(method_url, params, method='GET', headers=headers)
            return utils.csv_data_to_dict_list(csv_data, ';')

    @staticmethod
    def market_payments_stat(service, from_trantime, to_trantime):
        with reporter.step(u"Зовём ручку market_payments_stat"):
            method_url = '{base_url}/market_payments_stat'.format(base_url=env.balance_env().medium_http_url)
            params = {
                'from_trantime': from_trantime,
                'to_trantime': to_trantime,
                'service_id': service.id
            }
            headers = utils_tvm.supply_tvm_ticket({})
            csv_data = call_http(method_url, params, method='GET', headers=headers)
            return utils.csv_data_to_dict_list(csv_data, ';')

    @staticmethod
    def get_payouts_by_purchase_token(purchase_token):
        with reporter.step(u"Зовём ручку market_payments_stat"):
            method_url = '{base_url}/get_payouts_by_purchase_token'.format(base_url=env.balance_env().medium_http_url)
            params = {
                'purchase_token': purchase_token,
            }
            headers = utils_tvm.supply_tvm_ticket({})
            json_data = call_http(method_url, params, method='GET', headers=headers, check_code=True)
            return json.loads(json_data)
