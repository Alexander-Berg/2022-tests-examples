# -*- coding: utf-8 -*-

import datetime

import yt.wrapper as yt
import yt.yson as yson
from yt.wrapper import YtClient

from btestlib import secrets

def dt_convert(date=datetime.datetime.today()):
    return date.strftime('%Y-%m-%d')

def str_to_dt_and_back_to_str(dt):
    dt = datetime.datetime.strptime(dt, '%Y-%m-%d')
    return dt.strftime('%Y-%m-%d')

def create_yt_client(cluster='hahn'):
    return YtClient(
            config={
                'token': secrets.get_secret(*secrets.Tokens.YT_OAUTH_TOKEN),
                'yamr_mode': {'create_recursive': True},
                'pickling': {'force_using_py_instead_of_pyc': True},
                'proxy': {'url': '%(cluster)s.yt.yandex.net' % {'cluster': cluster}},
            }
    )


def write_table(table_path, data):
    client = create_yt_client()
    client.write_table(table_path, data, format=yt.JsonFormat())


def read_table(table_path):
    client = create_yt_client()
    table = yt.TablePath(table_path)
    return list(client.read_table(table, format=yt.JsonFormat()).rows)


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

def check_100_pct(data):
    total = 0
    for key, value in data[0].items():
        if key not in ('dt', 'login'):
            total += value
    assert total == 100, "Sum should be equal to 100"

def check_data_for_date(path, dt):
    current_data = read_table(path)

    for row in current_data:
        assert row['dt'] != dt, "Data for this day already exists"