import base64
from datetime import datetime
import json

from yatest.common import canonical_file as yatest_canonical_file
from maps.infopoint.points_for_period.lib.common import (
    write_points
)
from maps.infopoint.points_for_period.lib.points_for_period import (
    get_points_for_period, TableNameType, MOSCOW_TIMEZONE_OFFSET
)


def prepare_tables(ytc, already_encoded):
    tables = ytc.list('')
    for table in tables:
        human_readable = ytc.read_table(table)
        index = table.find('T')
        if index >= 0:
            part = table[index:]
            part = part.replace('-', ':')
            table = table[:index] + part

        if not already_encoded:
            data = []
            for row in human_readable:
                row['value'] = base64.b64encode(
                    json.dumps(row['value']).encode())
                data.append(row)
            ytc.write_table(table, data)


def run_test(ytc,
             begin=None,
             end=None,
             kwargs=None,
             already_encoded=False,
             canon_filename='output'):
    if not kwargs:
        kwargs = {}

    if 'table_name_type' not in kwargs:
        kwargs['table_name_type'] = TableNameType.DAYS
    if 'events_log_yt_folder' not in kwargs:
        kwargs['events_log_yt_folder'] = ''
    if 'use_begin_end_bound' not in kwargs:
        kwargs['use_begin_end_bound'] = False
    if 'only_new' not in kwargs:
        kwargs['only_new'] = False

    if not begin:
        begin = datetime(2019, 5, 7, 0, 0, 0)
    if not end:
        end = datetime(2019, 5, 7, 23, 59, 59)

    begin_utc = begin - MOSCOW_TIMEZONE_OFFSET
    end_utc = end - MOSCOW_TIMEZONE_OFFSET

    prepare_tables(ytc, already_encoded)

    json_filename = '{}.json'.format(canon_filename)
    table_filename = '{}.table'.format(canon_filename)
    with open(json_filename, 'w') as output:
        write_points(get_points_for_period(ytc,
                                           begin_utc,
                                           end_utc,
                                           format="json",
                                           **kwargs),
                     output)

    with open(table_filename, 'w') as output:
        write_points(get_points_for_period(ytc,
                                           begin_utc,
                                           end_utc,
                                           format="table",
                                           **kwargs),
                     output)

    return [yatest_canonical_file(json_filename, local=True),
            yatest_canonical_file(table_filename, local=True)]


def test_simple(ytc):
    ytc.config['prefix'] = '//simple/'
    return run_test(ytc, end=datetime(2019, 5, 8, 23, 59, 59))


def test_prod_bad_symbols(ytc):
    ytc.config['prefix'] = '//prod_bad_symbols/'
    # https://st.yandex-team.ru/MAPSCORE-4824
    return run_test(ytc, already_encoded=True)


def test_languages(ytc):
    ytc.config['prefix'] = '//languages/'
    return run_test(ytc)


def test_begin_end_filter(ytc):
    ytc.config['prefix'] = '//begin_end_filter/'
    kwargs = {
        'use_begin_end_bound': True
    }
    return run_test(ytc, kwargs=kwargs)


def test_tables_range(ytc):
    ytc.config['prefix'] = '//tables_range/'
    return run_test(ytc)


def test_only_new(ytc):
    ytc.config['prefix'] = '//only_new/'
    kwargs = {
        'only_new': True
    }
    return run_test(ytc, kwargs=kwargs)


def test_one_hour(ytc):
    ytc.config['prefix'] = '//one_hour/'
    return run_test(ytc,
                    begin=datetime(2019, 5, 7, 10, 0, 0),
                    end=datetime(2019, 5, 7, 11, 0, 0))


def test_hours_tables(ytc):
    ytc.config['prefix'] = '//hours_tables/'
    kwargs = {
        'table_name_type': TableNameType.HOURS
    }
    return run_test(ytc,
                    begin=datetime(2019, 5, 7, 6, 0, 0),
                    end=datetime(2019, 5, 7, 10, 0, 0),
                    kwargs=kwargs)


def test_region_filter(ytc):
    ytc.config['prefix'] = '//region_filter/'
    kwargs = {
        'region_filter': 213
    }
    return run_test(ytc, kwargs=kwargs)


def test_tag_filter(ytc):
    ytc.config['prefix'] = '//tag_filter/'
    kwargs = {
        'tag_filter': "police"
    }
    return run_test(ytc, kwargs=kwargs)


def test_only_shown(ytc):
    ytc.config['prefix'] = '//only_shown/'
    kwargs = {
        'only_shown': True,
        'operation_types': ["point_added", "point_shown"],
    }
    return run_test(ytc, kwargs=kwargs)


def test_operation_types(ytc):
    ytc.config['prefix'] = '//operation_types/'
    kwargs = {
        'operation_types': ["point_shown", "vote"],
    }
    return run_test(ytc, kwargs=kwargs)
