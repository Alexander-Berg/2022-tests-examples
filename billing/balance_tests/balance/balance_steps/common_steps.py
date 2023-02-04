# coding=utf-8
__author__ = 'igogor'

import datetime
import dateutil
import exceptions
import pickle
import pprint
import time
import xml.etree.ElementTree as ET
import xmlrpclib
import pytz
from decimal import Decimal

from hamcrest import has_length, greater_than_or_equal_to, equal_to, not_none

import balance.balance_api as api
import balance.balance_db as db
import btestlib.environments as env
import btestlib.reporter as reporter
import btestlib.utils as utils
from btestlib.data import defaults

log_align = 30
# log = reporter.logger()

# еще дефолтные даты есть в btestlib.data.defaults.Date
to_iso = utils.Date.date_to_iso_format
NOW = datetime.datetime.now()
NOW_ISO = to_iso(NOW)
HALF_YEAR_AFTER_NOW_ISO = to_iso(NOW + datetime.timedelta(days=180))
HALF_YEAR_BEFORE_NOW_ISO = to_iso(NOW - datetime.timedelta(days=180))
A_DAY_AFTER_TOMORROW = NOW + datetime.timedelta(days=2)


class CommonSteps(object):
    # TODO wait for the sync method of all queues (gavrilov@)
    # todo-blubimov этому методу место в ExportSteps
    # blubimov для вызовов с очередью OEBS лучше использовать ExportSteps#export_oebs, т.к. он делает несколько попыток выгрузки
    @staticmethod
    def export(queue_, classname, object_id, with_enqueue=False, priority=0, input_=None, next_export=None):
        '''
        Export object with [classname] type and [object_id] id to [queue_] queue.
        N\B: only for 'OEBS' queue now
        '''
        if with_enqueue:
            api.test_balance().Enqueue(classname, object_id, queue_)
        with reporter.step(u'Запускаем синхронный разбор очереди {0} для объекта {1} класса {2}'.format(
                queue_, object_id, classname)):
            result = api.test_balance().ExportObject(queue_, classname, object_id, priority, input_, next_export)
            # igogor убрал т.к. дублирует то что аттачится в методе
            # reporter.attach(u'Результат разбора', utils.Presenter.pretty(result))
        # log.debug('Export {0} object: {1} in {2} queue'.format(classname, object_id, queue_))
        return result

    # TODO: Refactor
    # TODO: Exception not from (Exception)
    @staticmethod
    def wait_for(query, query_params, value=1, interval=5, timeout=300):
        '''
        Wait for specified sql query result
        interval=5, timeout=300
        '''
        # sql = 'select value as val from bo.t_table where param = param_value'
        with reporter.step(u'Ожидаем значение {0} в запросе {1}'.format(query, query_params)):
            timer = 0
            while timer < timeout:
                time.sleep(interval)
                timer += interval
                cur_val = db.balance().execute(query, query_params)[0]['val']
                reporter.logger().debug('{0} sec: {1}'.format(interval, cur_val))
                if cur_val == value: return 'Waiting for {0} sec'.format(timer)
            raise Exception(
                "BTestlib exception: 'Timeout in {0} sec'\nWaiting for: {1}\nParams: {2}".format(timeout, query,
                                                                                                 query_params))

    @staticmethod
    def _get_db_with_name_by_schema(schema):
        if schema == 'balance':
            return db.balance(), 'bo'
        elif schema == 'bs':
            return db.balance_bs(), 'bs'
        else:
            raise ValueError('Unknown db "{}"'.format(schema))

    # todo-blubimov отбираем значения без classname, и берем из результатов первый попавшийся,
    # поэтому если в очереди несколько типов объектов, то можем ждать не тот что нужно
    # todo-blubimov этому методу место в ExportSteps
    @staticmethod
    def wait_for_export(type, object_id, interval=5, timeout=300, wait_for_error=0, schema='balance', wait_rate=None):
        with reporter.step(u'Ожидаем значение обработки объекта {0} в t_export с типом {1}'.format(object_id, type)):
            if wait_rate is None:
                wait_rate = 1

            # a-vasin: сука, траст
            if schema == 'bs' and env.SimpleapiEnvironment.DB_NAME in [env.TrustDbNames.BS_PG,
                                                                       env.TrustDbNames.BS_PG_DEV,
                                                                       env.TrustDbNames.BS_XG,
                                                                       env.TrustDbNames.BS_XG_DEV]:
                type_column_name = 'obj_type'
            else:
                type_column_name = 'type'

            query = "SELECT state, rate, error, traceback FROM t_export " \
                    "WHERE {} = :type AND object_id = :object_id".format(type_column_name)
            query_params = {'object_id': object_id, 'type': type}

            active_db, active_db_name = CommonSteps._get_db_with_name_by_schema(schema)

            def get_export_state():
                params = active_db.execute(query, query_params,
                                           descr='Ищем объект в {}.t_export'.format(active_db_name))
                if not params:
                    raise ValueError('Found nothing for query "{}" with params "{}"'.format(query, query_params))
                params = params[0]
                cur_val, rate, error, traceback = params['state'], params['rate'], params['error'], params['traceback']
                error_str = error if error is not None and error != 'None' else traceback
                print('{0} sec: {1}'.format(interval, cur_val))
                if (cur_val == 1 and schema == 'balance') or (cur_val == 2 and schema == 'bs'):
                    return 'Object has been exported'

                # a-vasin: https://st.yandex-team.ru/TRUST-3610
                if error and 'ORA-00001: unique constraint (BO.T_ORDER_PK2) violated' in error:
                    return None

                if rate >= wait_rate and traceback is not None and wait_for_error == 0:
                    raise utils.PredicateStateIncorrect(
                        'Object cannot be exported because of error: {0}'.format(error_str), cur_val)
                if rate >= wait_rate and traceback is not None and wait_for_error == 1:
                    return error_str

            return utils.wait_until(predicate=get_export_state,
                                    success_condition=not_none(),
                                    timeout=timeout, sleep_time=interval)

    @staticmethod
    def wait_for_value(query, query_params, interval=5, timeout=300, schema='balance'):
        timer = 0
        result = None
        active_db, active_db_name = CommonSteps._get_db_with_name_by_schema(schema)
        while timer < timeout:
            if result is None:
                time.sleep(interval)
                timer += interval
                result = active_db.execute(query, query_params)
                if not result:
                    result = None
                else:
                    result = result[0]['value']
                reporter.logger().debug('{0} sec: {1}'.format(interval, result))
            else:
                return result
        raise utils.ConditionHasNotOccurred(
            "Timeout in {0} sec\nWaiting for: {1}\nParams: {2}".format(timeout, query, query_params), result)

    @staticmethod
    def get_last_notification(opcode, object_id):
        notifications = api.test_balance().GetNotification(opcode, object_id)
        return notifications[-1]['args'][0] if len(notifications) > 0 else None

    @staticmethod
    def get_last_old_notification(opcode, object_id):
        notifications = api.test_balance().GetNotification(opcode, object_id)
        return notifications[-1]['args'] if len(notifications) > 0 else None

    @staticmethod
    def build_notification(opcode, object_id):
        return api.test_balance().GetNotificationInfo(opcode, object_id)

    @staticmethod
    def wait_and_get_notification(opcode, object_id, number, interval=5, timeout=300):
        return utils.wait_until(lambda: api.test_balance().GetNotification(opcode, object_id),
                                has_length(greater_than_or_equal_to(number)),
                                timeout=timeout, sleep_time=interval)[number - 1]['args'][0]

    @staticmethod
    def wait_and_get_old_notification(opcode, object_id, number, interval=5, timeout=300):
        return utils.wait_until(lambda: api.test_balance().GetNotification(opcode, object_id),
                                has_length(greater_than_or_equal_to(number)),
                                timeout=timeout, sleep_time=interval)[number - 1]['args']

    @staticmethod
    def get_host():
        return api.test_balance().GetHost()

    @staticmethod
    def get_firm_by_id(firm_id):
        try:
            query = 'SELECT * FROM bo.t_firm where id=:firm_id'
            query_params = {'firm_id': firm_id}
            result = db.balance().execute(query, query_params)[0]
            return result
        except:
            return None

    @staticmethod
    def get_service_by_id(service_id):
        try:
            query = 'SELECT * FROM bo.t_service where id=:service_id'
            query_params = {'service_id': service_id}
            result = db.balance().execute(query, query_params)[0]
            return result
        except:
            return None

    @staticmethod
    def get_trust_service_by_id(service_id):
        try:
            query = 'SELECT * FROM bo.t_trust_service WHERE id=:service_id'
            query_params = {'service_id': service_id}
            result = db.balance().execute(query, query_params)[0]
            return result
        except:
            #raise Exception(u"Error while '{0}' get_trust_service_by_id".format(service_id))
            return None

    @staticmethod
    def restart_pycron_task(task_name):
        try:
            query = 'SELECT state_id FROM v_pycron WHERE name = :task_name'
            query_params = {'task_name': task_name}
            state_id = db.balance().execute(query, query_params)[0]['state_id']
            db.balance().execute('UPDATE (SELECT * FROM t_pycron_state WHERE id = :state_id) SET started = NULL',
                                 {'state_id': state_id})
            return 'Task {0} successfully restarted'.format(task_name)
        except:
            raise Exception(u"Error while '{0}' pycron task restart".format(task_name))

    @staticmethod
    def restart_pycron_task_and_wait_for_export(task_name, export_type, object_id):
        CommonSteps.restart_pycron_task(task_name)
        CommonSteps.wait_for_export(export_type, object_id)

    @staticmethod
    def is_pycron_task_terminated(task_name):
        with reporter.step(u'Получаем значение флага terminate для задачи {}'.format(task_name)):
            return bool(db.balance().execute("SELECT terminate FROM t_pycron_descr WHERE name = :task_name",
                                             {'task_name': task_name}, single_row=True)['terminate'])

    @staticmethod
    def change_pycron_task_terminate_flag(task_name, terminate_flag):
        with reporter.step(u"Устанавливаем terminate = {} для задачи {}".format(terminate_flag, task_name)):
            query = "UPDATE t_pycron_descr SET terminate = :terminate_flag WHERE name = :task_name"
            db.balance().execute(query, {'task_name': task_name, 'terminate_flag': terminate_flag})

    @staticmethod
    def parse_notification(opcode, object_id, lvl1=None, lvl2=None, lvl3=None):
        curr_val = api.test_balance().GetNotification(opcode, object_id)
        if (lvl1 or lvl2 or lvl3):
            return curr_val[lvl1][lvl2][lvl3]
        else:
            return curr_val

    @staticmethod
    def parse_notification2(opcode, object_id, list_of_keys_or_indexes=None):
        curr_val = api.test_balance().GetNotification(opcode, object_id)
        if list_of_keys_or_indexes:
            for key_or_index in list_of_keys_or_indexes:
                curr_val = curr_val[key_or_index]
            return curr_val
        else:
            return curr_val

    @staticmethod
    def get_pickled_value(query, key='input'):
        '''
        Get pickled value from DB ([key] column) and loads it
        key='input'
        '''
        input_ = db.balance().execute(query)[0]['input']
        if input_ == None:
            return input_
        # Waiting for the value like: select input fromt_export where type = 'UA_TRANSFER' and classname = 'Order' and object_id = <id>
        query = query.replace("'", "\'")
        query = query.replace(key, "UTL_ENCODE.BASE64_ENCODE({0}) as key1".format(key))
        value = db.balance().execute(query)[0]['key1']
        return pickle.loads(value.decode('base64'))

    @staticmethod
    def make_pickled_value(data):
        '''
        Pickle data and convert it to plain statement for insert/update
        '''
        pickled_data = pickle.dumps(data)
        pickled_list = pickled_data.split('\n')
        for index, row in enumerate(pickled_list):
            pickled_list[index] = "'" + row.replace("'", "''") + "'"
        result = ('UTL_RAW.CAST_TO_RAW(' + '||chr(10)||'.join(pickled_list) + ')')
        return result

    @staticmethod
    # TODO to 'get' or not to 'get'
    def next_sequence_id(sequence_name):
        '''
        Get nex id from [sequence_name] sequence
        '''
        query = 'select {0}.nextval from dual'.format(sequence_name)
        return db.balance().execute(query)[0]['nextval']

    @staticmethod
    def set_extprops(classname, object_id, attrname, params=None, insert_force=False,
                     passport_id=defaults.PASSPORT_UID):
        '''
        Set [attribute] value to extprops of the object of [classname] type and [object_id] id
        params should contain one of 'value_num', 'value_key', 'value_dt', 'value_srt' or 'value_json'
        '''
        dict = defaults.extprops()
        if params:
            params = utils.keys_to_lowercase(params)
            dict.update(params)

        query = 'SELECT id FROM t_extprops WHERE classname = :classname AND attrname = :attrname AND object_id = :object_id'
        result = db.balance().execute(query, {'classname': classname, 'attrname': attrname, 'object_id': object_id})

        if result and not insert_force:
            dict = utils.remove_empty(dict)
            query = ('update T_EXTPROPS set {0} where ID={1}'.format(
                (', '.join('{0}={1}'.format(k, v) for k, v in dict.items())), result[0]['id']))
        else:
            dict.update({'id': CommonSteps.next_sequence_id('S_EXTPROPS'),
                         'classname': classname,
                         'object_id': object_id,
                         'attrname': attrname})
            dict = utils.remove_empty(dict)
            for k in dict.keys():
                if k in ['classname', 'attrname', 'key', 'value_str']:
                    dict[k] = "'" + dict[k] + "'"
            query = (
                'insert into T_EXTPROPS ({0}) values ({1})'.format((', '.join('{}'.format(k) for k in dict.keys())),
                                                                   (', '.join('{}'.format(v) for v in dict.values()))))
        db.balance().execute(query)
        return

    @staticmethod
    def get_extprops(classname, object_id, attrname=None):
        if attrname is not None:
            query = 'SELECT * FROM t_extprops WHERE classname = :classname AND attrname = :attrname AND object_id = :object_id'
            result = db.balance().execute(query, {'classname': classname, 'attrname': attrname, 'object_id': object_id})
        else:
            query = 'SELECT * FROM t_extprops WHERE classname = :classname AND object_id = :object_id'
            result = db.balance().execute(query, {'classname': classname, 'object_id': object_id})
        # TODO need to be redesigned
        return result
        # params = {'classname': classname, 'object_id': object_id}
        # if attrname:
        #     params['attrname'] = attrname
        # result = db.oracle_select('t_extprops', ['*'], params)
        # return result

    @staticmethod
    def get_exception_code(exc, tag_name='code'):
        '''
        try:
            pass
        except Exception, exc:
            if 'PERSON_TYPE_MISMATCH' == steps.CommonSteps.get_exception_code(exc):
                pass
        '''
        if isinstance(exc, xmlrpclib.Fault):
            try:
                exc_text = ET.fromstring(exc.faultString.encode('utf-8'))
            except ET.ParseError:
                return exc.faultString.encode('utf-8')
        elif isinstance(exc, exceptions.Exception):
            exc_text = ET.fromstring(exc.message)
        else:
            if exc.faultString.startswith('Error: DatabaseError'):
                return 'Error: DatabaseError'
        exc_code = exc_text.find(tag_name).text
        return exc_code

    # todo-architect это не степ, а утилитный метод
    @staticmethod
    def log(f):
        def cut(s):
            CUT_LIMIT = 1500
            return s[:CUT_LIMIT - 3] + u'...' if len(s) > CUT_LIMIT else s

        def smart_repr(obj):
            return pprint.pformat(obj).decode('unicode_escape')

        def new_logging_func(*args, **kwargs):
            argformat = []
            import xmlrpclib

            if isinstance(f, xmlrpclib._Method):
                formatparams = [f._Method__name]
            else:
                formatparams = [f.func_name]
            if args:
                argformat.append(u'%s')
                formatparams.append(', '.join([smart_repr(arg) for arg in args]))
            if kwargs:
                argformat.append(u'**kwargs = %s')
                formatparams.append(smart_repr(kwargs))
            format = u'Вызов: %s(' + u', '.join(argformat) + ')'
            reporter.log(cut(format % tuple(formatparams)))
            result = f(*args, **kwargs)
            reporter.log(cut(u'Ответ: %s' % smart_repr(result)))
            print
            return result

        return new_logging_func

    @staticmethod
    def log2(f):
        def cut(s):
            CUT_LIMIT = 1500
            return s[:CUT_LIMIT - 3] + u'...' if len(s) > CUT_LIMIT else s

        def smart_repr(obj):
            return pprint.pformat(obj).decode('unicode_escape')

        def new_logging_func(*args, **kwargs):
            argformat = []
            import xmlrpclib

            if isinstance(f, xmlrpclib._Method):
                formatparams = [f._Method__name]
            else:
                formatparams = [f.func_name]
            if args:
                argformat.append(u'%s')
                formatparams.append(', '.join([smart_repr(arg) for arg in args]))
            if kwargs:
                argformat.append(u'**kwargs = %s')
                formatparams.append(smart_repr(kwargs))
            format = u'Вызов: %s(' + u', '.join(argformat) + ')'
            reporter.log(cut(format % tuple(formatparams)))
            result = f(*args, **kwargs)
            reporter.log(cut(u'Ответ: %s' % smart_repr(result)))
            print
            return result

        return new_logging_func

    @staticmethod
    def increase_priority(classname, object_id, type):
        # query_update_state = "update t_export set priority = -1 where classname = :classname and type = :type and object_id = :object_id"
        # query_params = {'classname': classname, 'type': type, 'object_id': object_id}
        # db.balance().execute(query_update_state, query_params)
        with reporter.step(u'Увеличиваем приоритет для {} id={} в очереди {}'.format(classname, object_id, type)):
            db.oracle_update('t_export', {'priority': -1},
                             {'classname': classname, 'type': type, 'object_id': object_id})
            # log.debug('Priority was increased for {0} with id={1} in the queue {2}'.format(classname, object_id, type))

    @staticmethod
    def get_latest_exchange_rate(from_currency, to_currency, exchange_date=None):
        with reporter.step(u"Получаем курс конвертации из {} в {}".format(from_currency.char_code,
                                                                          to_currency.char_code)):
            if from_currency == to_currency:
                exchange_rate = Decimal(1)
            else:
                if not exchange_date:
                    exchange_date = datetime.datetime.now()

                query = 'SELECT * FROM mv_distr_currency_rate ' \
                        'WHERE base_cc=:base_cc AND cc=:cc AND rate_src_id=:rate_src_id AND from_dt=' \
                        '(SELECT max(from_dt) FROM mv_distr_currency_rate ' \
                        'WHERE base_cc=:base_cc AND cc=:cc AND rate_src_id=:rate_src_id AND from_dt<=:exchange_date)'
                params = {'base_cc': from_currency.char_code, 'cc': to_currency.char_code,
                          'rate_src_id': from_currency.central_bank_code, 'exchange_date': exchange_date}

                exchange_rate_info = db.balance().execute(query, params)[0]

                exchange_rate = Decimal(1) / Decimal(exchange_rate_info['rate'])

            reporter.attach(u"Курс", utils.Presenter.pretty(exchange_rate))
            return exchange_rate

    @staticmethod
    def check_exception(exc, data):
        return utils.check_that(CommonSteps.get_exception_code(exc, 'msg'), equal_to(data))

    @staticmethod
    def get_nds_pct_on_dt(dt, nds_id=18):
        pct = db.balance().execute(
            ''' select nds_pct from v_nds_pct where ndsreal_id = :nds_id
                and from_dt <= :dt and to_dt > :dt
            ''', dict(dt=dt, nds_id=nds_id))[0]['nds_pct']
        return pct

    @staticmethod
    def format_dt_msk2utc(_dt):
        msk_tz = dateutil.tz.gettz('Europe/Moscow')
        if not _dt:
            return _dt
        return _dt.replace(tzinfo=msk_tz).astimezone(pytz.utc).isoformat() if _dt else None
