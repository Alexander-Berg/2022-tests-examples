# coding: utf-8
__author__ = 'chihiro'
from datetime import datetime, timedelta

import six

import yt.wrapper as yt
import yt.yson as yson
from yt.wrapper.ypath import ypath_join, ypath_dirname
from yt.wrapper.errors import YtHttpResponseError
from retrying import retry
from dateutil.relativedelta import relativedelta

from btestlib import secrets


def _create_yt_client(proxy_url='hahn.yt.yandex.net'):
    return yt.YtClient(config={
        'token': secrets.get_secret(*secrets.Tokens.YT_OAUTH_TOKEN),
        'proxy': {
            'url': proxy_url,
        },
    })


def _sbt_create_table(client, path):
    client.remove(path, force=True)
    schema = [
        {
            "name": "client_id",
            "required": False,
            "type": "string"
        },
        {
            "name": "currency",
            "required": False,
            "type": "string"
        },
        {
            "name": "payment_id",
            "required": False,
            "type": "string"
        },
        {
            "name": "type",
            "required": False,
            "type": "string"
        },
        {
            "name": "updated",
            "required": False,
            "type": "double"
        },
        {
            "name": "value",
            "required": False,
            "type": "string"
        },
        {
            "name": "payment_method",
            "required": False,
            "type": "string"
        }
    ]
    client.create(
        'table',
        yt.TablePath(path),
        attributes={
            'schema': yson.to_yson_type(
                schema,
                attributes={'strict': True}
            )
        }
    )


def sbt_create_data_in_yt(data):
    client = _create_yt_client()
    client.mkdir('//home/balance_reports/dcs/test/test_data', recursive=True)
    client.mkdir('//home/balance_reports/dcs/test/test_data/taxi', recursive=True)
    client.remove('//home/balance_reports/dcs/test/test_data/taxi/subsidies', recursive=True, force=True)
    client.mkdir('//home/balance_reports/dcs/test/test_data/taxi/subsidies', recursive=True)
    path = '//home/balance_reports/dcs/test/test_data/taxi/subsidies/{}'.format(datetime.now().strftime("%Y-%m-%d"))
    _sbt_create_table(client, path)
    client.write_table(
        path,
        data,
        format=yt.JsonFormat(attributes={
            # Так работает поддержка utf8, см. документацию YT
            "encode_utf8": False
        }))


def _cbd_create_table(client, path):
    client.remove(path, force=True)
    schema = [
        {
            'name': 'client_id',
            'required': False,
            'type': 'string',
        },
        {
            'name': 'commission_sum',
            'required': False,
            'type': 'int64',
        },
        {
            'name': 'dt',
            'required': False,
            'type': 'string',
        },
        {
            'name': 'orig_transaction_id',
            'required': False,
            'type': 'string',
        },
        {
            'name': 'payment_type',
            'required': False,
            'type': 'string',
        },
        {
            'name': 'promocode_sum',
            'required': False,
            'type': 'string',
        },
        {
            'name': 'service_order_id',
            'required': False,
            'type': 'string',
        },
        {
            'name': 'total_sum',
            'required': True,
            'type': 'string',
        },
        {
            'name': 'transaction_currency',
            'required': True,
            'type': 'string',
        },
        {
            'name': 'transaction_id',
            'required': False,
            'type': 'string',
        },
        {
            'name': 'type',
            'required': True,
            'type': 'string',
        },
        {
            'name': 'use_discount',
            'required': False,
            'type': 'int64',
        },
    ]
    client.create(
        'table',
        yt.TablePath(path),
        attributes={
            'schema': yson.to_yson_type(
                schema,
                attributes={'strict': True}
            )
        }
    )


def _obg2_create_table(client, path):
    client.remove(path, force=True)
    schema = [
        {
            'name': 'order_id',
            'required': False,
            'type': 'int64',
        },
        {
            # 'name': 'completion_delta',
            'name': 'spent_delta',
            'required': True,
            'type': 'string',
        },
    ]
    client.create(
        'table',
        yt.TablePath(path),
        attributes={
            'schema': yson.to_yson_type(
                schema,
                attributes={'strict': True}
            )
        }
    )


def cbd_create_data_in_yt(data):
    client = _create_yt_client()
    path = '//home/balance_reports/dcs/test/test_data/drive/transactions'

    client.remove(path, recursive=True, force=True)
    client.mkdir(path, recursive=True)

    day_path_template = yt.ypath.ypath_join(path, '{}')

    for day, completions in six.iteritems(data):
        day_path = day_path_template.format(day)
        _cbd_create_table(client, day_path)
        client.write_table(
            day_path,
            completions,
            format=yt.JsonFormat(attributes={
                # Так работает поддержка utf8, см. документацию YT
                "encode_utf8": False
            }))

def obg2_create_data_in_yt(data):
    client = _create_yt_client()
    path = '//home/balance_reports/dcs/test/test_data/geo/logs'

    client.remove(path, recursive=True, force=True)
    client.mkdir(path, recursive=True)

    day_path_template = yt.ypath.ypath_join(path, '{}')

    for day, completions in six.iteritems(data):
        day_path = day_path_template.format(day)
        _obg2_create_table(client, day_path)
        client.write_table(
            day_path,
            completions,
            format=yt.JsonFormat(attributes={
                # Так работает поддержка utf8, см. документацию YT
                "encode_utf8": False
            }))


def obg_create_data_in_yt(data):
    client = _create_yt_client()
    path = '//home/balance_reports/dcs/test/test_data/geo/transactions'

    client.remove(path, recursive=True, force=True)
    client.mkdir(path, recursive=True)

    day_path_template = yt.ypath.ypath_join(path, '{}')

    for day, completions in six.iteritems(data):
        day_path = day_path_template.format(day)
        _obg_create_table(client, day_path)
        client.write_table(
            day_path,
            completions,
            format=yt.JsonFormat(attributes={
                # Так работает поддержка utf8, см. документацию YT
                "encode_utf8": False
            }))


def _cbt_create_table(client, path):
    """
    Для ускорения процесса и уменьшения количества кода объединил
     два формата сверки в один. Это неправильно и со временем желательно
     переделать.
    """
    client.remove(path, recursive=True, force=True)
    schema = [
        # Общие поля
        {
            "name": "client_id",
            "required": False,
            "type": "string"
        },
        # Поля сверки до транзакционого лога
        {
            "name": "type",
            "required": False,
            "type": "string"
        },
        {
            "name": "order_cost",
            "required": False,
            "type": "string"
        },
        {
            "name": "coupon_value",
            "required": False,
            "type": "string"
        },
        {
            "name": "payment_method",
            "required": False,
            "type": "string"
        },
        {
            "name": "commission_value",
            "required": False,
            "type": "string"
        },
        {
            "name": "commission_currency",
            "required": False,
            "type": "string"
        },
        # Поля для transaction log
        {
            "name": "service_id",
            "required": False,
            "type": "int32"
        },
        {
            "name": "currency",
            "required": False,
            "type": "string"
        },
        {
            "name": "transaction_type",
            "required": False,
            "type": "string"
        },
        {
            "name": "amount",
            "required": False,
            "type": "string"
        },
        {
            "name": "event_time",
            "required": False,
            "type": "string"
        },
        {
            "name": "product",
            "required": False,
            "type": "string"
        },
        {
            "name": "ignore_in_balance",
            "required": False,
            "type": "boolean"
        },
        {
            "name": "aggregation_sign",
            "required": False,
            "type": "int8"
        }
    ]
    client.create(
        'table',
        yt.TablePath(path),
        attributes={
            'schema': yson.to_yson_type(
                schema,
                attributes={'strict': True}
            )
        }
    )


def cbt_create_data_in_yt(data):
    client = _create_yt_client()

    completions_path = '//home/balance_reports/dcs/test/test_data/taxi/completions'
    client.remove(completions_path, recursive=True, force=True)
    client.mkdir(completions_path, recursive=True)

    for file_date, file_data in data.iteritems():
        if isinstance(file_date, datetime):
            file_date = file_date.strftime('%Y-%m-%d')

        table_path = ypath_join(completions_path, file_date)
        _cbt_create_table(client, table_path)
        client.write_table(
            table_path,
            file_data,
            format=yt.JsonFormat(attributes={
                # Так работает поддержка utf8, см. документацию YT
                "encode_utf8": False
            }))


def _obb2_create_table(client, path):
    client.remove(path, force=True)
    schema = [
        {
            "name": "BillingExportID",
            "required": True,
            "type": "int64",
            "sort_order": "ascending"
        },
        {
            "name": "OrderID",
            "required": True,
            "type": "int64",
            "sort_order": "ascending"
        },
        {
            "name": "EngineID",
            "required": True,
            "type": "int64"
        },
        {
            "name": "Days",
            "required": True,
            "type": "int64"
        },
        {
            "name": "Shows",
            "required": True,
            "type": "int64"
        },
        {
            "name": "Clicks",
            "required": False,
            "type": "int64"
        },
        {
            "name": "Cost",
            "required": True,
            "type": "int64"
        },
        {
            "name": "CostCur",
            "required": False,
            "type": "int64"
        },
        {
            "name": "CostFinal",
            "required": True,
            "type": "int64"
        },
        {
            "name": "CurrencyRatio",
            "required": True,
            "type": "int64"
        },
    ]
    client.create(
        'table',
        yt.TablePath(path),
        attributes={
            'dynamic': True,
            'optimize_for': 'scan',
            'schema': yson.to_yson_type(
                schema,
                attributes={'strict': True}
            )
        }
    )
    client.mount_table(path)


def _cpbt_create_table(client, path):
    client.remove(path, force=True)
    schema = [
        {
            "name": "client_id",
            "required": False,
            "type": "string"
        },
        {
            "name": "currency",
            "required": False,
            "type": "string"
        },
        {
            "name": "payment_id",
            "required": False,
            "type": "string"
        },
        {
            "name": "type",
            "required": False,
            "type": "string"
        },
        {
            "name": "postauth_dt",
            "required": False,
            "type": "double"
        },
        {
            "name": "value",
            "required": False,
            "type": "string"
        }
    ]
    client.create(
        'table',
        yt.TablePath(path),
        attributes={
            'schema': yson.to_yson_type(
                schema,
                attributes={'strict': True}
            )
        }
    )


def _dc_taxi_distr_create_table(client, path):
    client.remove(path, force=True)
    schema = [
        {
            "name": "utc_dt",
            "required": False,
            "type": "string",
        },
        {
            "name": "clid",
            "required": False,
            "type": "string",
        },
        {
            "name": "product_id",
            "required": False,
            "type": "string"
        },
        {
            "name": "commission",
            "required": False,
            "type": "string"
        },
        {
            "name": "cost",
            "required": False,
            "type": "string"
        },
        {
            "name": "quantity",
            "required": False,
            "type": "string"
        },
    ]
    client.create(
        'table',
        yt.TablePath(path),
        attributes={
            'schema': yson.to_yson_type(
                schema,
                attributes={'strict': True}
            )
        }
    )


def _dc_activations_create_table(client, path):
    client.remove(path, force=True)
    schema = [
        {
            "name": "billing_period",
            "required": False,
            "type": "string",
        },
        {
            "name": "product_id",
            "required": False,
            "type": "int32",
        },
        {
            "name": "clid",
            "required": False,
            "type": "int64"
        },
        {
            "name": "vid",
            "required": False,
            "type": "int32"
        },
        {
            "name": "count",
            "required": False,
            "type": "uint64"
        },
    ]
    client.create(
        'table',
        yt.TablePath(path),
        attributes={
            'schema': yson.to_yson_type(
                schema,
                attributes={'strict': True}
            )
        }
    )


@retry(stop_max_attempt_number=5, wait_exponential_multiplier=1 * 1000, retry_on_exception=YtHttpResponseError)
def obb2_create_data_in_yt(data):
    client = _create_yt_client()

    table_path = '//home/balance_reports/dcs/test/test_data/bk/BillingOrderStat'

    client.remove(table_path, recursive=True, force=True)
    client.mkdir(ypath_dirname(table_path), recursive=True)

    _obb2_create_table(client, table_path)
    # client.write_table(
    client.insert_rows(
        table_path,
        data,
        format=yt.JsonFormat(attributes={
            # Так работает поддержка utf8, см. документацию YT
            "encode_utf8": False
        }))


def cpbt_create_data_in_yt(data):
    client = _create_yt_client()

    path = '//home/balance_reports/dcs/test/test_data/taxi/corp_payments'

    client.remove(path, recursive=True, force=True)
    client.mkdir(path, recursive=True)

    path = '//home/balance_reports/dcs/test/test_data/taxi/corp_payments/{}'.format(datetime.now().strftime("%Y-%m-%d"))
    _cpbt_create_table(client, path)
    client.write_table(
        path,
        data,
        format=yt.JsonFormat(attributes={
            # Так работает поддержка utf8, см. документацию YT
            "encode_utf8": False
        }))


def dc_taxi_distr_create_data_in_yt(data):
    client = _create_yt_client()

    path = '//home/balance_reports/dcs/test/test_data/taxi/taxi_distr'

    client.remove(path, recursive=True, force=True)
    client.mkdir(path, recursive=True)

    path = '{}/{}'.format(path, datetime.now().strftime("%Y-%m-%d"))
    _dc_taxi_distr_create_table(client, path)
    client.write_table(
        path,
        data,
        format=yt.JsonFormat(attributes={
            # Так работает поддержка utf8, см. документацию YT
            "encode_utf8": False
        }))


def dc_activations_create_data_in_yt(path, file_date, data):
    client = _create_yt_client()

    client.remove(path, recursive=True, force=True)
    client.mkdir(path, recursive=True)

    path = '{}/{}'.format(path, file_date.strftime("%Y-%m-%d"))
    _dc_activations_create_table(client, path)
    client.write_table(
        path,
        data,
        format=yt.JsonFormat(attributes={
            # Так работает поддержка utf8, см. документацию YT
            "encode_utf8": False
        }))


def _tbs_create_table(client, path):
    client.remove(path, force=True)
    schema = [{
                "name": "client_id",
                "required": False,
                "type": "string"
            },
            {
                "name": "amount",
                "required": True,
                "type": "int32"
            },
            {
                "name": "transaction_type",
                "required": True,
                "type": "string"
            },
            {
                "name": "service_id",
                "required": True,
                "type": "string"
            },
            {
                "name": "payment_type",
                "required": True,
                "type": "string"
            },
            {
                "name": "payload",
                "required": False,
                "type": "string"
            },
            {
                "name": "currency",
                "required": True,
                "type": "string"
            },
            {
                "name": "transaction_id",
                "required": False,
                "type": "string"
            },
            {
                "name": "dt",
                "required": True,
                "type": "string"
            }]
    client.create(
        'table',
        yt.TablePath(path),
        attributes={
            'schema': yson.to_yson_type(
                schema,
                attributes={'strict': True}
            )
        }
    )


def tbs_create_data_in_yt(data):
    client = _create_yt_client()

    path = '//home/balance_reports/dcs/test/test_data/taxi/scouts'

    client.remove(path, recursive=True, force=True)
    client.mkdir(path, recursive=True)

    path = '//home/balance_reports/dcs/test/test_data/taxi/scouts/{}'.format(datetime.now().strftime("%Y-%m-%d"))
    _tbs_create_table(client, path)
    client.write_table(
        path,
        data,
        format=yt.JsonFormat(attributes={
            # Так работает поддержка utf8, см. документацию YT
            "encode_utf8": False
        }))


def _prcbb_create_table(client, path):
    client.remove(path, force=True)
    schema = [
        {
            "name": "pageid",
            "type": "string"
        },
        {
            "name": "eventtime",
            "type": "string"
        },
        {
            "name": "unixtime",
            "type": "string"
        },
        {
            "name": "win",
            "type": "string"
        },
        {
            "name": "dspfraudbits",
            "type": "string"
        },
        {
            "name": "dspeventflags",
            "type": "string"
        },
        {
            "name": "countertype",
            "type": "string"
        },
        {
            "name": "price",
            "type": "string"
        },
        {
            "name": "partnerprice",
            "type": "string"
        }
    ]
    client.create(
        'table',
        yt.TablePath(path),
        attributes={
            'schema': yson.to_yson_type(
                schema,
                attributes={'strict': True}
            )
        }
    )

def prcbb_create_data_in_yt(data):
    client = _create_yt_client()
    reverse_date = (datetime.now() - relativedelta(months=1)).replace(day=3)


    # client.mkdir('//home/balance_reports/dcs/test/test_data', recursive=True)
    # client.mkdir('//home/balance_reports/dcs/test/test_data/bs-undodsp-log', recursive=True)
    client.remove('//home/balance_reports/dcs/test/test_data/bs-undodsp-log/1d', recursive=True, force=True)
    client.mkdir('//home/balance_reports/dcs/test/test_data/bs-undodsp-log/1d', recursive=True)
    client.remove('//home/balance_reports/dcs/test/test_data/bs-undodsp-log/5m', recursive=True, force=True)
    client.mkdir('//home/balance_reports/dcs/test/test_data/bs-undodsp-log/5m', recursive=True)
    _prcbb_create_table(client, '//home/balance_reports/dcs/test/test_data/bs-undodsp-log/5m/{}'.format(
        (datetime.now() + timedelta(hours=1)).strftime("%Y-%m-%dT%H:30:00")))
    path = '//home/balance_reports/dcs/test/test_data/bs-undodsp-log/1d/{}'.format(
        reverse_date.strftime("%Y-%m-%d"))
    _prcbb_create_table(client, path)
    client.write_table(
        path,
        data,
        format=yt.JsonFormat(attributes={
            # Так работает поддержка utf8, см. документацию YT
            "encode_utf8": False
        }))


def _cbf_create_table(client, path):
    client.remove(path, force=True)
    schema = [
        {
            'name': 'transaction_id',
            'type': 'int64'
        },
        {
            'name': 'client_id',
            'type': 'int64'
        },
        {
            'name': 'commission_sum',
            'type': 'string'
        },
        {
            'name': 'dt',
            'type': 'string'
        },
        {
            'name': 'orig_transaction_id',
            'type': 'int64'
        },
        {
            'name': 'promocode_sum',
            'type': 'string'
        },
        {
            'name': 'service_id',
            'type': 'int64'
        },
        {
            'name': 'service_order_id',
            'type': 'string'
        },
        {
            'name': 'total_sum',
            'type': 'string'
        },
        {
            'name': 'transaction_currency',
            'type': 'string'
        },
        {
            'name': 'type',
            'type': 'string'
        },
        {
            'name': 'utc_start_load_dttm',
            'type': 'string'
        },
    ]
    client.create(
        'table',
        yt.TablePath(path),
        attributes={
            'schema': yson.to_yson_type(
                schema,
                attributes={'strict': True}
            )
        }
    )


def cbf_create_data_in_yt(file_date, data):
    client = _create_yt_client()

    path = '//home/balance_reports/dcs/test/test_data/food/commissions'

    client.remove(path, recursive=True, force=True)
    client.mkdir(path, recursive=True)

    file_path = ypath_join(path, file_date)
    _cbf_create_table(client, file_path)

    client.write_table(
        file_path,
        data,
        format=yt.JsonFormat(attributes={
            # Так работает поддержка utf8, см. документацию YT
            "encode_utf8": False
        }))


def _pbf_create_table(client, path):
    client.remove(path, force=True)
    schema = [
        {
            'name': 'transaction_id',
            'type': 'int64'
        },
        {
            'name': 'client_id',
            'type': 'int64'
        },
        {
            'name': 'dt',
            'type': 'string'
        },
        {
            'name': 'payload',
            'type': 'string'
        },
        {
            'name': 'payment_id',
            'type': 'int64'
        },
        {
            'name': 'payment_type',
            'type': 'string'
        },
        {
            'name': 'paysys_type_cc',
            'type': 'string'
        },
        {
            'name': 'product',
            'type': 'string'
        },
        {
            'name': 'service_id',
            'type': 'int64'
        },
        {
            'name': 'service_order_id',
            'type': 'string'
        },
        {
            'name': 'transaction_type',
            'type': 'string'
        },
        {
            'name': 'utc_start_load_dttm',
            'type': 'string'
        },
        {
            'name': 'value_amount',
            'type': 'string'
        },
        {
            'name': 'value_currency',
            'type': 'string'
        },
    ]
    client.create(
        'table',
        yt.TablePath(path),
        attributes={
            'schema': yson.to_yson_type(
                schema,
                attributes={'strict': True}
            )
        }
    )


def pbf_create_data_in_yt(file_date, data):
    client = _create_yt_client()

    path = '//home/balance_reports/dcs/test/test_data/food/payments'

    client.remove(path, recursive=True, force=True)
    client.mkdir(path, recursive=True)

    file_path = ypath_join(path, file_date)
    _pbf_create_table(client, file_path)

    client.write_table(
        file_path,
        data,
        format=yt.JsonFormat(attributes={
            # Так работает поддержка utf8, см. документацию YT
            "encode_utf8": False
        }))


def _dc_reverse_create_table(client, path):
    client.remove(path, force=True)
    schema = [
        {
            "name": "pageid",
            "type": "string"
        },
        {
            "name": "placeid",
            "type": "string"
        },
        {
            "name": "eventtime",
            "type": "string"
        },
        {
            "name": "unixtime",
            "type": "string"
        },
        {
            "name": "typeid",
            "type": "string"
        },
        {
            "name": "options",
            "type": "string"
        },
        {
            "name": "partnerstatid",
            "type": "string"
        },
        {
            "name": "tagid",
            "type": "string"
        },
        {
            "name": "resourcetype",
            "type": "string"
        },
        {
            "name": "fraudbits",
            "type": "string"
        },
        {
            "name": "countertype",
            "type": "string"
        },
        {
            "name": "oldeventcost",
            "type": "string"
        },
        {
            "name": "neweventcost",
            "type": "string"
        }
    ]
    client.create(
        'table',
        yt.TablePath(path),
        attributes={
            'schema': yson.to_yson_type(
                schema,
                attributes={'strict': True}
            )
        }
    )


def dc_reverse_create_data_in_yt(data):
    reverse_date = (datetime.now() - relativedelta(months=1)).replace(day=3)
    client = yt.YtClient(config={
        'token': secrets.get_secret(*secrets.Tokens.YT_OAUTH_TOKEN),
        'proxy': {
            'url': 'hahn.yt.yandex.net'
        },
    })
    client.remove('//home/balance_reports/dcs/test/test_data/bs-undochevent-log', recursive=True, force=True)
    client.mkdir('//home/balance_reports/dcs/test/test_data/bs-undochevent-log/1d', recursive=True)
    client.mkdir('//home/balance_reports/dcs/test/test_data/bs-undochevent-log/5m', recursive=True)
    _dc_reverse_create_table(client, '//home/balance_reports/dcs/test/test_data/bs-undochevent-log/5m/{}'.format(
        (datetime.now() + timedelta(hours=1)).strftime("%Y-%m-%dT%H:30:00")))
    path = '//home/balance_reports/dcs/test/test_data/bs-undochevent-log/1d/{}'.format(
        reverse_date.strftime("%Y-%m-%d"))
    _dc_reverse_create_table(client, path)
    client.write_table(
        path,
        data,
        format=yt.JsonFormat(attributes={
            # Так работает поддержка utf8, см. документацию YT
            "encode_utf8": False
        }))


def _zbb_boid_create_table(client, path):
    client.remove(path, force=True)

    schema = [
        {
            'name': 'cid',
            'type': 'int64'
        },
        {
            'name': 'wallet_cid',
            'type': 'int64'
        },
    ]
    client.create(
        'table',
        yt.TablePath(path),
        attributes={
            'schema': yson.to_yson_type(
                schema,
                attributes={'strict': True}
            )
        }
    )


def zbb_boid_create_data_in_yt(data):
    table_path = '//home/balance_reports/dcs/test/test_data/bk/billing_aggregates'

    client = _create_yt_client()
    client.remove(table_path, recursive=True, force=True)
    client.mkdir(ypath_dirname(table_path), recursive=True)
    _zbb_boid_create_table(client, table_path)

    client.write_table(
        table_path,
        data,
        format=yt.JsonFormat(attributes={
            # Так работает поддержка utf8, см. документацию YT
            "encode_utf8": False
        })
    )


def taxi_transaction_log_create_data_in_yt(create_table_func, table_path, data):
    client = _create_yt_client()

    client.mkdir(table_path, recursive=True)

    for file_date, file_data in data.iteritems():
        if isinstance(file_date, datetime):
            file_date = file_date.strftime('%Y-%m-%d')

        table_path = ypath_join(table_path, file_date)
        create_table_func(client, table_path)
        client.write_table(
            table_path,
            file_data,
            format=yt.JsonFormat(attributes={
                # Так работает поддержка utf8, см. документацию YT
                "encode_utf8": False
            }))


def taxi_revenues_create_yt_table(client, table_path):
    client.remove(table_path, force=True)
    schema = [
        {
            "name": "client_id",
            "required": False,
            "type": "string"
        },
        {
            "name": "service_id",
            "required": False,
            "type": "int32"
        },
        {
            "name": "currency",
            "required": False,
            "type": "string"
        },
        {
            "name": "transaction_type",
            "required": False,
            "type": "string"
        },
        {
            "name": "amount",
            "required": False,
            "type": "string"
        },
        {
            "name": "event_time",
            "required": False,
            "type": "string"
        },
        {
            "name": "product",
            "required": False,
            "type": "string"
        },
        {
            "name": "ignore_in_balance",
            "required": False,
            "type": "boolean"
        },
        {
            "name": "aggregation_sign",
            "required": False,
            "type": "int8"
        }
    ]
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


def taxi_expenses_create_yt_table(client, path):
    client.remove(path, force=True)
    schema = [
        {
            "name": "transaction_id",
            "required": False,
            "type": "int64"
        },
        {
            "name": "service_id",
            "required": False,
            "type": "int32"
        },
        {
            "name": "client_id",
            "required": False,
            "type": "string"
        },
        {
            "name": "product",
            "required": False,
            "type": "string"
        },
        {
            "name": "transaction_type",
            "required": False,
            "type": "string"
        },
        {
            "name": "amount",
            "required": False,
            "type": "string"
        },
        {
            "name": "currency",
            "required": False,
            "type": "string"
        },
        {
            "name": "event_time",
            "required": False,
            "type": "string"
        },
        {
            "name": "transaction_time",
            "required": False,
            "type": "string"
        },
        {
            "name": "ignore_in_balance",
            "required": False,
            "type": "boolean"
        }
    ]
    client.create(
        'table',
        yt.TablePath(path),
        attributes={
            'schema': yson.to_yson_type(
                schema,
                attributes={'strict': True}
            )
        }
    )


def taxi_revenues_create_data_in_yt(table_path, data):
    taxi_transaction_log_create_data_in_yt(taxi_revenues_create_yt_table,
                                           table_path, data)


def taxi_expenses_create_data_in_yt(table_path, data):
    taxi_transaction_log_create_data_in_yt(taxi_expenses_create_yt_table,
                                           table_path, data)
