# -*- coding: utf-8 -*-

import yt.wrapper as yt
from yt.wrapper import YtClient
import yt.yson as yson
import datetime

cluster = 'hahn'

def dt_convert(date=datetime.datetime.today()):
    return date.strftime('%Y-%m-%d')

def create_yt_client():
    return YtClient(
        config={
            'yamr_mode': {'create_recursive': True},
            'pickling': {'force_using_py_instead_of_pyc': True},
            'proxy': {'url': '%(cluster)s.yt.yandex.net' % {'cluster': cluster}},
        }
    )

def write_test_table(table_path, data):
    client = create_yt_client()
    client.write_table(table_path, data, format=yt.JsonFormat())

def create_table(table_path, schema):
    client = create_yt_client()
    client.create(
        'table',
        yt.TablePath(table_path),
        attributes={
            'schema': yson.to_yson_type(
                schema,
                attributes={'strict': True}
            )
        }
    )

def create_table_test_cases_ci_stat():
    table_path = '//home/balance-test/metrics/test_cases_ci_stat'
    schema = [
        {
            'name': 'dt',
            'required': True,
            'type': 'string',
        },
        {
            'name': 'total',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'smoke_total',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'assessors_total',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'automated_total',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'manual_total',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'assessors_of_smoke',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'assessors_of_manual',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'manual_of_smoke',
            'required': True,
            'type': 'int64',
        },
    ]
    create_table(table_path, schema)

def create_table_test_cases_ai_stat():
    table_path = '//home/balance-test/metrics/test_cases_ai_stat'
    schema = [
        {
            'name': 'dt',
            'required': True,
            'type': 'string',
        },
        {
            'name': 'total',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'smoke_total',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'assessors_total',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'automated_total',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'manual_total',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'react_total',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'automated_react',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'automated_of_smoke',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'automated_react_of_smoke',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'automated_of_manual',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'automated_react_of_manual',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'smoke_react',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'manual_of_smoke',
            'required': True,
            'type': 'int64',
        },
    ]
    create_table(table_path, schema)

def fill_test_cases_ci_stat():
    # ПЕРЕТИРАЕТ ДАННЫЕ В ТАБЛИЦЕ, чтобы добавлять параметр append
    table_path = '//home/balance-test/metrics/test_cases_ci_stat'
    data = [
        {
            'dt': dt_convert(datetime.datetime(2020,9,30)),
            'assessors_of_smoke': 179,
            'manual_of_smoke': 375,
            'assessors_total': 293,
            'assessors_of_manual': 78,
            'total': 2197,
            'manual_total': 236,
            'smoke_total': 759,
            'automated_total': 0,
        }
    ]
    write_test_table(table_path, data)

def fill_test_cases_ai_stat():
    # ПЕРЕТИРАЕТ ДАННЫЕ В ТАБЛИЦЕ, чтобы добавлять параметр append
    table_path = '//home/balance-test/metrics/test_cases_ai_stat'
    data = [
        {
            'dt': dt_convert(datetime.datetime(2020,9,30)),
            'automated_react_of_smoke': 0,
            'automated_react': 0,
            'automated_of_smoke': 168,
            'manual_of_smoke': 0,
            'automated_react_of_manual': 0,
            'assessors_total': 0,
            'total': 3846,
            'manual_total': 514,
            'automated_total': 376,
            'automated_of_manual': 127,
            'react_total': 0,
            'smoke_total': 965,
            'smoke_react': 0,
        }
    ]
    write_test_table(table_path, data)

# create_table_test_cases_ci_stat()
# create_table_test_cases_ai_stat()

def create_table_duty_stat():
    table_path = '//home/balance-test/metrics/backend_duty_stat'
    schema = [
        {
            'name': 'dt',
            'required': True,
            'type': 'string',
        },
        {
            'name': 'monitorings',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'system_tests_analysis',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'system_tests_fix',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'unit_tests_analysis',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'unit_tests_fix',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'db_reload',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'releases',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'services_help',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'other_duty',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'not_duty',
            'required': True,
            'type': 'int64',
        },
    ]
    create_table(table_path, schema)


def create_table_front_duty_stat():
    table_path = '//home/balance-test/metrics/frontend_duty_stat'
    schema = [
        {
            'name': 'dt',
            'required': True,
            'type': 'string',
        },
        {
            'name': 'monitorings_error_booster',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'releases',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'hermione_tests_analysis',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'unit_tests_analysis',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'other_duty',
            'required': True,
            'type': 'int64',
        },
        {
            'name': 'not_duty',
            'required': True,
            'type': 'int64',
        },
    ]
    create_table(table_path, schema)

def fill_duty_backend_stat():
    # ПЕРЕТИРАЕТ ДАННЫЕ В ТАБЛИЦЕ, если не передать параметр append
    dst = yt.TablePath('//home/balance-test/metrics/backend_duty_stat', append=True)
    data = [
        {
            'dt': dt_convert(datetime.datetime(2020,12,7)),
            'monitorings': 10,
            'system_tests_analysis': 20,
            'system_tests_fix': 0,
            'unit_tests_analysis': 0,
            'unit_tests_fix': 5,
            'db_reload': 10,
            'releases': 20,
            'services_help': 10,
            'other_duty': 15,
            'not_duty': 10,
        }
    ]
    write_test_table(dst, data)

def fill_duty_frontend_stat():
    # ПЕРЕТИРАЕТ ДАННЫЕ В ТАБЛИЦЕ, если не передать параметр append
    dst = yt.TablePath('//home/balance-test/metrics/frontend_duty_stat', append=True)
    data = [
        {
            'dt': dt_convert(datetime.datetime(2020,12,7)),
            'monitorings_error_booster': 13,
            'releases': 3,
            'hermione_tests_analysis': 10,
            'unit_tests_analysis': 1,
            'other_duty': 1,
            'not_duty': 72,
        }
    ]
    write_test_table(dst, data)

# fill_duty_backend_stat()
# fill_duty_frontend_stat()