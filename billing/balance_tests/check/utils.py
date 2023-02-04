# coding=utf-8

import datetime
import os
import time
import json
import uuid

import pytest
import hamcrest
import requests
import retrying
import startrek_client
from dateutil.relativedelta import relativedelta

import btestlib.reporter as reporter
from btestlib import utils as b_utils
from balance import balance_api as api
from balance.balance_db import balance
from check import db as check_db
from check.defaults import LAST_DAY_OF_PREVIOUS_MONTH, DATA_DIR, \
    STARTREK_PARAMS
from btestlib import environments, shared

LAST_DAY_OF_MONTH = 32


def dcs_api_url():
    env = environments.balance_env()
    return 'https://yb-dcs-{}.paysys.yandex.net:8024/api'.format(env.name)


def relative_date(start_date=None, truncate=True, **relativedelta_kwargs):
    start_date = start_date or datetime.datetime.now()
    if truncate:
        start_date = datetime.datetime.combine(start_date, datetime.time())
    return start_date + relativedelta(**relativedelta_kwargs)


@retrying.retry(stop_max_attempt_number=3, wait_exponential_multiplier=1 * 1000)
def run_check(check_code, objects, raw_cmd_args=''):
    print(u'Запуск сверки ' + str(check_code))
    command = [str(check_code), '--objects', str(objects)]
    if raw_cmd_args is not '':
        command += raw_cmd_args.split(' ')
    api.test_balance().DCSRunCheck(command)
    return check_db.get_cmp_id(check_code)


def run_check_new(check_code, objects=None, params=None, **kwargs):
    reporter.log(u'Запуск сверки: {}'.format(str(check_code)))

    if params is None:
        params = kwargs.copy()
    else:
        params = params.copy()

    params['code'] = str(check_code)

    if objects:
        if not isinstance(objects, basestring):
            objects = ','.join(map(str, objects))
        params['objects'] = objects

    reporter.log(u'Параметры запуска: {}'.format(str(check_code)))

    # Из-за того, что не стали менять test_xmlprc приходится прятать параметры в список
    # Возможно, в будущем стоит поменять это поведение
    t_run_id = api.test_balance().DCSRunCheckNew([json.dumps(params)])

    # Дожидаемся окончания сверки
    with reporter.step(u'Дожидаемся окончания запуска: cmp.t_run.id = {}'.format(t_run_id)):
        b_utils.wait_until(
            lambda: check_db.is_check_run_finished(t_run_id),
            hamcrest.is_(True),
            # TODO: вынести в конфиг?
            timeout=10 * 60,
            sleep_time=5
        )

    # TODO: вынести в функцию
    query = """
      select cmp_id, state, json_value(check_params, '$.need_auto_analysis' returning number) has_auto_analysis
      from t_run
      where id = :run_id
    """
    res = api.test_balance().ExecuteSQL('cmp', query, {'run_id': t_run_id})[0]
    cmp_id, cmp_state, has_auto_analysis = res['cmp_id'], res['state'], res['has_auto_analysis']

    # TODO: вынести в константы
    if not cmp_id or cmp_state == 3:
        reporter.log(u'Сверка упала с неизвестной ошибкой')
        raise RuntimeError(u'check run failed with unknown error')

    reporter.log(u'ID сверки: {}'.format(cmp_id))

    # Если у сверки ожидается авторазбор, пытаемся проверить его выполнение
    if has_auto_analysis:
        with reporter.step(u'Дожидаемся окончания работы авторазбора: cmp.t_auto_analysis_run_info.cmp_id = {}'.format(cmp_id)):
            # Небольшая пауза перед проверкой
            time.sleep(5)
            b_utils.wait_until(
                lambda: check_db.is_auto_analysis_running(check_code, cmp_id),
                # TODO: вынести в конфиг?
                hamcrest.is_(False),
                timeout=10 * 60,
                sleep_time=5,
            )
    return cmp_id


def run_auto_analyze(check_code, cmp_id):
    requests.post(
        dcs_api_url() + '/testing/auto_analyze',
        json=[check_code, str(cmp_id)],
        verify=False
    ).raise_for_status()


# TODO ускорить (use 'export_object' method (should be fixed by gavrilovp@))
# хранится здесь на всякий случай, пока метод, починенный Пашей, не работает
def oebs_export(export_type, object_id):
    query = 'update (select  * from t_export where  CLASSNAME = :export_type and   type=\'OEBS\' and OBJECT_ID = :object_id ) set priority = -1'
    query_params = {'export_type': export_type, 'object_id': object_id}
    balance().execute(query, query_params)
    state = 0
    reporter.log(str(export_type) + ' export begin:' + str(datetime.datetime.now()))
    while True:
        if state == 0:
            query = 'select  state from t_export where  CLASSNAME = :export_type and   type=\'OEBS\' and OBJECT_ID = :object_id '
            query_params = {'export_type': export_type, 'object_id': object_id}
            state = balance().execute(query, query_params)[0]['state']
            print('...(3)')
            time.sleep(3)
        else:
            reporter.log(str(export_type) + ' export   end:' + str(datetime.datetime.now()))
            break


def expired_date(act_list, type_):
    key_1 = act_list.keys()[0]
    descr = act_list[key_1].keys()[0]
    act_id = act_list[str(key_1)][str(descr)]['id']
    query = 'select dt from bo.t_{0} where id= :act_id'.format(type_.lower())
    query_params = {'act_id': act_id}
    rows = balance().execute(query, query_params)
    if len(rows) > 0:
        dt = rows[0]['dt']
        if dt.date() < LAST_DAY_OF_PREVIOUS_MONTH.date():
            return True
    else:
        return True
    return False


def need_data_regeneration(acts_list_cache, type_):
    # --force: flag for data re-generation.
    # usage: --force
    arg = pytest.config.getoption("--force")
    force = True if arg else False
    if acts_list_cache is not None:
        expired = expired_date(acts_list_cache, type_)
        return expired or force
    else:
        return True


def parse_checks(groups):
    check_groups = {'aob_all': ['aob_sw', 'aob_tr', 'aob_us', 'aob', 'aob_ua', 'aob_taxi', 'aob_vertical'],
                    'iob_all': ['iob_sw', 'iob_tr', 'iob_us', 'iob', 'iob_ua', 'iob_taxi', 'iob_vertical']
                    }

    arg = pytest.config.getoption("--checklist")
    if not arg:
        return None
    check_group_names = arg.split(',')

    checks = []
    for group_name in check_group_names:
        if group_name in check_groups[groups]:
            checks.extend(group_name)
    return checks


def ensure_data_dir():
    if not os.path.exists(DATA_DIR):
        os.mkdir(DATA_DIR)


def create_data_file(file_name, content=''):
    ensure_data_dir()
    file_path = os.path.join(DATA_DIR, file_name)
    reporter.log(file_path)
    with open(file_path, 'w') as fh:
        fh.write(content)
    return file_path


def create_data_file_in_s3(content, file_name=None, file_path=None, db_key=None,
                           url_formatter=None):
    if not file_name:
        file_name = uuid.uuid4().hex

    if not file_path:
        file_path = uuid.uuid4().hex

    item = '{}/{}'.format(file_path, file_name)

    reporter.log('>>>>>[ITEM]: {}'.format(item))
    reporter.log('>>>>>[CONTENT]: {}'.format(content))

    url = shared.push_raw_data_to_s3_and_get_url(item, content)
    if url_formatter:
        url = url_formatter(url)

    reporter.log('>>>>>[URL]: {}'.format(url))
    b_utils.check_that(url, hamcrest.contains_string('balance-autotest-shared'), 'Проверяем, что в S3 сгенерировался URL')

    if db_key:
        query = 'update t_config set value_str = :value where item = :item'
        api.test_balance().ExecuteSQL('cmp', query, {'value': url, 'item': db_key})

    return url


def zbb_create_data_file_for_direct(content):
    item = 'zbb_direct_importer_url'

    url = create_data_file_in_s3(
        content=content,
        file_name='direct_paid_orders_not_in_bk.json',
    )
    value_json = json.dumps([url, None])

    query = 'update t_config set value_json = :value where item = :item'
    api.test_balance().ExecuteSQL('cmp', query, {'value': value_json, 'item': item})
    return url


def str_date(date):
    return date.strftime('%Y-%m-%d')


def get_check_ticket(check_code_name, cmp_id):
    query = 'select issue_key from {}_cmp where id = {}'. \
        format(check_code_name, cmp_id)
    issue_key = api.test_balance().ExecuteSQL('cmp', query)[0]['issue_key']
    return startrek_client.Startrek(**STARTREK_PARAMS).issues[issue_key]


def get_tracker_queue(queue_name):
    return startrek_client.Startrek(**STARTREK_PARAMS).queues.get(queue_name, expand=['team'])


def get_db_config_value(item_key):
    query = "select * from t_config where item = '{}'".format(item_key)
    item = api.test_balance().ExecuteSQL('cmp', query)[0]
    for key in ('dt', 'num', 'str', 'json'):
        value = item.get('value_' + key)
        if value is not None:
            if key == 'json':
                value = json.loads(value)
            return value
