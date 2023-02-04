import collections
import datetime
import time
import re

import mock
import pytest
import json

from google.protobuf import timestamp_pb2
import ydb
from infra.swatlib import metrics
from infra.dproxy.proto import dproxy_pb2
from infra.dproxy.src.ydb_logs import controller, queries
from infra.dproxy.src.ydb_logs.ydbutil import DateTablesGenerator, OlapTablesGenerator
from infra.dproxy.src.ydb_logs.timeutil import microseconds_to_dt, dt_to_str, \
    datetime_from_ts_us, start_of_day


TS = (int(time.time()) - 36 * 60 * 60) * 1000000  # now - 36 hours, in ydb microseconds format
TS_DATE = datetime.datetime.utcfromtimestamp(TS // 1000000).date()
INCLUDE = True
EXCLUDE = False


@pytest.fixture(autouse=True)
def patch_json_format(monkeypatch):
    monkeypatch.setattr("google.protobuf.json_format.MessageToDict", lambda message: mock.Mock())


def ts_to_ydb_negative_ms(ts):
    return -abs(ts)


def ts_to_ydb_timestamp(ts):
    return ts


def make_row(
    ts,
    context='',
    ts_converter=ts_to_ydb_negative_ms
):
    return dict(
        timestamp=ts_converter(ts),
        container_id='container-id',
        host='host',
        pod='pod',
        box='box',
        workload='workload',
        logger_name='logger-name',
        user_id='user-id',
        request_id='request-id',
        message='message',
        log_level='log-level',
        log_level_int=42,
        pod_transient_fqdn='pod-transient-fqdn',
        pod_persistent_fqdn='pod-persistent-fqdn',
        node_fqdn='node-fqdn',
        stack_trace='stack-trace',
        thread_name='thread-name',
        context=context,
        seq=abs(ts),
    )


def olap_make_row(
    ts,
    context='',
):
    return make_row(
        ts=ts,
        context=context,
        ts_converter=ts_to_ydb_timestamp,
    )


def awacs_make_row(ts):
    return dict(
        timestamp_us=ts_to_ydb_negative_ms(ts),
        env_type='UNKNOWN',
        domain='domain',
        upstream='upstream',
        client_ip='client_ip',
        client_port=80,
        hostname='hostname',
        cookies=json.dumps({
            'A': 'B'
        }),
        headers=json.dumps({
            'X-A': '123'
        }),
        yandexuid='yandexuid',
        method='method',
        process_time=0.123,
        reason=None,
        request='request',
        request_id='request_id',
        status=200,
        workflow='workflow',
        pushclient_row_id=1
    )


def assert_ts(
    resp,
    rows,
    ts_converter=ts_to_ydb_negative_ms,
    ts_field=queries.QueryBuilder.TIMESTAMP_FLD,
):
    expected_ts = [ts_converter(e.timestamp.ToMicroseconds()) for e in resp.log_entries]
    result_ts = [r[ts_field] for r in rows]
    assert expected_ts == result_ts


def clean_query(q):
    return re.sub(r'\s+', ' ', q).strip()


class FakeSessionPool(object):
    def __init__(self, session):
        self.session = session

    def retry_operation_sync(self, f, *args, **kwargs):
        return f(self.session, *args, **kwargs)


FakeDirectoryItem = collections.namedtuple('FakeDirectoryItem', ('name', 'type'))
FakeListDirectoryResponse = collections.namedtuple('FakeListDirectoryResponse', ('children',))


def make_list_directory(from_date, days, forward=False):
    mul = 1 if forward else -1
    return FakeListDirectoryResponse(children=[
        FakeDirectoryItem(
            name=DateTablesGenerator.make_table_name_from_dt(from_date + datetime.timedelta(days=mul * x)),
            type=ydb.SchemeEntryType.TABLE,
        )
        for x in range(days)
    ])


def make_olap_list_directory():
    return FakeListDirectoryResponse(children=[
        FakeDirectoryItem(
            name='log',
            type=ydb.SchemeEntryType.COLUMN_TABLE,
        )
    ])


def test_search_single_table():
    expected_q = clean_query(
        """
        PRAGMA TablePathPrefix("/foo/bar");
        PRAGMA AnsiInForEmptyOrNullableItemsCollections;
        DECLARE $log_level AS Utf8;
        DECLARE $message0 AS Utf8;
        DECLARE $log_level_int AS List<Int64>;
        DECLARE $timestamp_begin AS Int64;
        DECLARE $timestamp_end AS Int64;
        $message0_re = Hyperscan::Grep($message0);
        SELECT `timestamp`, `container_id`, `host`, `pod`, `box`, `workload`,
               `logger_name`, `user_id`, `request_id`, `message`, `log_level`, `seq`, `context`,
               `pod_transient_fqdn`, `pod_persistent_fqdn`, `node_fqdn`, `stack_trace`, `thread_name`,
               `log_level_int`
        FROM `{ts_date}`
        WHERE `log_level` == $log_level
              AND ($message0_re(`message`))
              AND `log_level_int` IN COMPACT $log_level_int
              AND `timestamp` <= $timestamp_begin
              AND `timestamp` >= $timestamp_end
        ORDER BY `timestamp` ASC, `container_id` ASC, `seq` ASC
        LIMIT 3;
        """.format(ts_date=DateTablesGenerator.make_table_name_from_dt(TS_DATE))
    )
    rows = []
    rows.append(make_row(TS))
    rows.append(make_row(TS - 1))
    rows.append(make_row(TS - 2))

    sc = mock.MagicMock(spec=ydb.SchemeClient)
    sc.list_directory.return_value = make_list_directory(TS_DATE, 10)
    tc = mock.MagicMock(spec=ydb.TableClient)
    tc.scan_query.return_value = [mock.Mock(result_set=mock.Mock(rows=rows))]

    reg = metrics.Registry()
    ctl = controller.YdbLogsController(scheme_client=sc,
                                       table_client=tc,
                                       metrics_registry=reg,
                                       history_tables_count=100,
                                       pool_size=1,
                                       request_timeout=1)
    from_ts = timestamp_pb2.Timestamp()
    from_ts.FromMicroseconds(TS + 1)
    resp = ctl.search(table_path='/foo/bar',
                      search_patterns=(['qwerty 90%'], INCLUDE),
                      timestamp_range=(None, from_ts),
                      log_levels=(['INFO'], INCLUDE),
                      log_levels_int=([100, 200], INCLUDE),
                      hosts=None,
                      pods=None,
                      boxes=None,
                      workloads=None,
                      containers=None,
                      logger_names=None,
                      pod_transient_fqdns=None,
                      pod_persistent_fqdns=None,
                      node_fqdns=None,
                      thread_names=None,
                      request_ids=None,
                      stack_traces=None,
                      user_fields=None,
                      continuation_token=None,
                      limit=3,
                      order=dproxy_pb2.DESC)

    assert_ts(resp, rows)

    calls = tc.method_calls
    assert len(calls) == 1
    n, args, kw = calls[0]
    assert n == 'scan_query'
    assert clean_query(args[0].yql_text) == expected_q

    args[1][u'$timestamp_begin'] = datetime_from_ts_us(-args[1][u'$timestamp_begin'])
    args[1][u'$timestamp_end'] = datetime_from_ts_us(-args[1][u'$timestamp_end'])
    dt = datetime_from_ts_us(TS + 1)

    assert args[1] == {u'$timestamp_end': dt,
                       u'$timestamp_begin': max(dt - datetime.timedelta(hours=1), start_of_day(dt)),
                       u'$message0': ur'(?i)\Qqwerty 90%\E',
                       u'$log_level': 'INFO',
                       u'$log_level_int': [100, 200],
                       }


def test_olap_search_single_table():
    rows = []
    rows.append(olap_make_row(TS))
    rows.append(olap_make_row(TS - 1))
    rows.append(olap_make_row(TS - 2))

    sc = mock.MagicMock(spec=ydb.SchemeClient)
    sc.list_directory.return_value = make_olap_list_directory()
    tc = mock.MagicMock(spec=ydb.TableClient)
    tc.scan_query.return_value = [mock.Mock(result_set=mock.Mock(rows=rows))]

    reg = metrics.Registry()
    ctl = controller.OlapYdbLogsController(scheme_client=sc,
                                           table_client=tc,
                                           metrics_registry=reg,
                                           history_tables_count=100,
                                           pool_size=1,
                                           request_timeout=1)

    end_ms = TS + 1
    end_dt = microseconds_to_dt(end_ms)
    begin_dt, _ = ctl.calc_min_max_possible_dates()
    order_pb = dproxy_pb2.DESC

    expected_q = clean_query(
        """
        PRAGMA Kikimr.EnableLlvm="false";
        PRAGMA Kikimr.KqpPushOlapProcess="true";
        PRAGMA TablePathPrefix("/foo/bar");
        PRAGMA AnsiInForEmptyOrNullableItemsCollections;
        DECLARE $log_level AS Utf8;
        DECLARE $message0 AS Utf8;
        DECLARE $log_level_int AS List<Int64>;
        $message0_re = Hyperscan::Grep($message0);
        $timestamp_begin = CAST("{ts_begin_str}" AS Timestamp);
        $timestamp_end = CAST("{ts_end_str}" AS Timestamp);
        SELECT `timestamp`, `container_id`, `host`, `pod`, `box`, `workload`,
               `logger_name`, `user_id`, `request_id`, `message`, `log_level`, `seq`, `context`,
               `pod_transient_fqdn`, `pod_persistent_fqdn`, `node_fqdn`, `stack_trace`, `thread_name`,
               `log_level_int`
        FROM `{olap_table_name}`
        WHERE `log_level` == $log_level
              AND ($message0_re(`message`))
              AND `log_level_int` IN COMPACT $log_level_int
              AND $timestamp_begin <= `timestamp`
              AND `timestamp` <= $timestamp_end
        ORDER BY `timestamp` {order}, `host` {order}, `seq` {order}
        LIMIT 3;
        """.format(
            ts_begin_str=dt_to_str(begin_dt),
            ts_end_str=dt_to_str(end_dt),
            olap_table_name=OlapTablesGenerator.TABLE_NAME,
            order=queries.ORDER_PB_TO_STR[order_pb],
        )
    )

    from_ts = timestamp_pb2.Timestamp()
    from_ts.FromMicroseconds(end_ms)
    resp = ctl.search(table_path='/foo/bar',
                      search_patterns=(['qwerty 90%'], INCLUDE),
                      timestamp_range=(None, from_ts),
                      log_levels=(['INFO'], INCLUDE),
                      log_levels_int=([100, 200], INCLUDE),
                      hosts=None,
                      pods=None,
                      boxes=None,
                      workloads=None,
                      containers=None,
                      logger_names=None,
                      pod_transient_fqdns=None,
                      pod_persistent_fqdns=None,
                      node_fqdns=None,
                      thread_names=None,
                      request_ids=None,
                      stack_traces=None,
                      user_fields=None,
                      continuation_token=None,
                      limit=3,
                      order=order_pb)

    assert_ts(resp, rows, ts_converter=ts_to_ydb_timestamp)

    calls = tc.method_calls
    assert len(calls) == 1
    n, args, kw = calls[0]
    assert n == 'scan_query'
    assert clean_query(args[0].yql_text) == expected_q

    assert args[1] == {u'$message0': ur'(?i)\Qqwerty 90%\E',
                       u'$log_level': 'INFO',
                       u'$log_level_int': [100, 200],
                       }


@pytest.mark.parametrize('requests,where', [
    (
        (['req.+', 'r'], INCLUDE, dproxy_pb2.GREP),
        clean_query(
            """
            WHERE `upstream` NOT IN COMPACT $upstream AND `hostname` NOT IN COMPACT $hostname
                AND `yandexuid` NOT IN COMPACT $yandexuid
                AND `method` == $method
                AND ($request0_re(`request`) OR $request1_re(`request`))
                AND `timestamp_us` <= $timestamp_begin AND `timestamp_us` >= $timestamp_end
            """
        )
    ),
    (
        (['req.+', 'r'], EXCLUDE, dproxy_pb2.GREP),
        clean_query(
            """
            WHERE `upstream` NOT IN COMPACT $upstream AND `hostname` NOT IN COMPACT $hostname
                AND `yandexuid` NOT IN COMPACT $yandexuid
                AND `method` == $method
                AND (NOT $request0_re(`request`) AND NOT $request1_re(`request`))
                AND `timestamp_us` <= $timestamp_begin AND `timestamp_us` >= $timestamp_end
            """
        )
    )
])
def test_awacs_search_single_table(requests, where):
    expected_q = clean_query(
        """
        PRAGMA TablePathPrefix("/foo/bar");
        PRAGMA AnsiInForEmptyOrNullableItemsCollections;
        DECLARE $upstream AS List<Utf8>;
        DECLARE $hostname AS List<Utf8>;
        DECLARE $yandexuid AS List<Utf8>;
        DECLARE $method AS Utf8;
        DECLARE $request0 AS Utf8;
        DECLARE $request1 AS Utf8;
        DECLARE $timestamp_begin AS Int64;
        DECLARE $timestamp_end AS Int64;
        $request0_re = Pire::Grep($request0);
        $request1_re = Pire::Grep($request1);
        SELECT `timestamp_us`, `pushclient_row_id`, `env_type`, `namespace`, `domain`, `upstream`, `client_ip`,
               `client_port`, `hostname`, `cookies`, `headers`,
               `yandexuid`, `method`, `process_time`, `reason`,
               `request`, `request_id`, `status`, `workflow`
        FROM `{ts_date}`
        {where}
        ORDER BY `timestamp_us` ASC, `hostname` ASC, `pushclient_row_id` ASC
        LIMIT 3;
        """.format(ts_date=DateTablesGenerator.make_table_name_from_dt(TS_DATE), where=where)
    )
    rows = []
    rows.append(awacs_make_row(TS))
    rows.append(awacs_make_row(TS - 1))
    rows.append(awacs_make_row(TS - 2))

    sc = mock.MagicMock(spec=ydb.SchemeClient)
    sc.list_directory.return_value = make_list_directory(TS_DATE, 10)
    tc = mock.MagicMock(spec=ydb.TableClient)
    tc.scan_query.return_value = [mock.Mock(result_set=mock.Mock(rows=rows))]

    reg = metrics.Registry()
    ctl = controller.AwacsYdbLogsController(scheme_client=sc,
                                            table_client=tc,
                                            metrics_registry=reg,
                                            history_tables_count=100,
                                            pool_size=1,
                                            request_timeout=1)
    from_ts = timestamp_pb2.Timestamp()
    from_ts.FromMicroseconds(TS + 1)

    resp = ctl.awacs_search(
        known_args=dict(
            table_path='/foo/bar',
            timestamp_range=(None, from_ts),
            env_types=None,
            domains=None,
            upstreams=(['upstream1', 'upstream2'], EXCLUDE, dproxy_pb2.GREP),  # GREP do nothing
            client_ips=None,
            client_ports=None,
            hostnames=(['hostname1', 'hostname2'], EXCLUDE),
            yandexuids=(['a', 'b'], EXCLUDE, dproxy_pb2.EQ),
            cookies=None,
            headers=None,
            methods=(['method'], INCLUDE, dproxy_pb2.EQ),
            reasons=None,
            request_ids=None,
            requests=requests,  # GREP make sense
            statuses=None,
            process_time_list=None,
            continuation_token=None,
            order=dproxy_pb2.DESC,
            limit=3,
        ),
        limit=3,
    )

    assert_ts(resp, rows, ts_field=queries.AwacsQueryBuilder.TIMESTAMP_FLD)

    calls = tc.method_calls
    assert len(calls) == 1
    n, args, kw = calls[0]
    assert n == 'scan_query'
    print(args[0].yql_text)
    # print(json.dumps(args[1],  indent=2))
    assert clean_query(args[0].yql_text) == expected_q

    args[1][u'$timestamp_begin'] = datetime_from_ts_us(-args[1][u'$timestamp_begin'])
    args[1][u'$timestamp_end'] = datetime_from_ts_us(-args[1][u'$timestamp_end'])
    dt = datetime_from_ts_us(TS + 1)
    assert args[1] == {
        '$method': 'method',
        '$timestamp_begin': max(dt - datetime.timedelta(hours=1), start_of_day(dt)),
        '$timestamp_end': dt,
        '$upstream': [
            'upstream1',
            'upstream2'
        ],
        '$request0': 'req.+',
        '$request1': 'r',
        '$yandexuid': [
            'a',
            'b'
        ],
        '$hostname': [
            'hostname1',
            'hostname2'
        ],
    }


def test_search_multiple_tables():
    expected_q_tpl = clean_query(
        """
        PRAGMA TablePathPrefix("/foo/bar");
        PRAGMA AnsiInForEmptyOrNullableItemsCollections;
        DECLARE $log_level AS Utf8;
        DECLARE $message0 AS Utf8;
        DECLARE $timestamp_begin AS Int64;
        DECLARE $timestamp_end AS Int64;
        $message0_re = Hyperscan::Grep($message0);
        SELECT `timestamp`, `container_id`, `host`, `pod`, `box`, `workload`,
               `logger_name`, `user_id`, `request_id`, `message`, `log_level`, `seq`, `context`,
               `pod_transient_fqdn`, `pod_persistent_fqdn`, `node_fqdn`, `stack_trace`, `thread_name`,
               `log_level_int`
        FROM `{table}`
        WHERE `log_level` == $log_level AND ($message0_re(`message`))
        AND `timestamp` <= $timestamp_begin
        AND `timestamp` >= $timestamp_end
        ORDER BY `timestamp` ASC, `container_id` ASC, `seq` ASC
        LIMIT 3;
        """
    )
    rows = []
    rows.append(make_row(TS))
    rows.append(make_row(TS - 1))
    rows.append(make_row(TS - 2))

    sc = mock.MagicMock(spec=ydb.SchemeClient)
    sc.list_directory.return_value = make_list_directory(TS_DATE, 10)
    tc = mock.MagicMock(spec=ydb.TableClient)
    tc.scan_query.side_effect = [
        [mock.Mock(result_set=mock.Mock(rows=rows[:1]))],
        [mock.Mock(result_set=mock.Mock(rows=[]))],
        [mock.Mock(result_set=mock.Mock(rows=[]))],
        [mock.Mock(result_set=mock.Mock(rows=rows[1:]))],
        [mock.Mock(result_set=mock.Mock(rows=[]))],
        [mock.Mock(result_set=mock.Mock(rows=[]))],
    ]
    reg = metrics.Registry()
    ctl = controller.YdbLogsController(scheme_client=sc,
                                       table_client=tc,
                                       metrics_registry=reg,
                                       history_tables_count=100,
                                       pool_size=1,
                                       request_timeout=1)

    from_ts = timestamp_pb2.Timestamp()
    from_ts.FromMicroseconds(TS + 1)
    resp = ctl.search(table_path='/foo/bar',
                      search_patterns=(['qwerty'], INCLUDE),
                      timestamp_range=(None, from_ts),
                      log_levels=(['INFO'], INCLUDE),
                      log_levels_int=None,
                      hosts=None,
                      pods=None,
                      boxes=None,
                      workloads=None,
                      containers=None,
                      logger_names=None,
                      pod_transient_fqdns=None,
                      pod_persistent_fqdns=None,
                      node_fqdns=None,
                      thread_names=None,
                      stack_traces=None,
                      request_ids=None,
                      user_fields=None,
                      continuation_token=None,
                      limit=3,
                      order=dproxy_pb2.DESC)

    assert_ts(resp, rows)

    calls = tc.method_calls
    assert len(calls) == 4

    end = datetime_from_ts_us(TS+1)
    expected_ts = []
    i = 0
    hours = (1, 3, 20)
    for _ in range(4):
        midnight = start_of_day(end)
        start = max(end - datetime.timedelta(hours=hours[i]), midnight)
        expected_ts.append((start, end))
        end = start
        if end == midnight:
            end = end - datetime.timedelta(microseconds=1)
            i = 0
        else:
            i += 1

    expected = [
        (
            'scan_query',
            {
                u'$timestamp_begin': dt_range[0],
                u'$timestamp_end': dt_range[1],
                u'$message0': ur'(?i)\Qqwerty\E',
                u'$log_level': 'INFO'
            },
           expected_q_tpl.format(table=DateTablesGenerator.make_table_name_from_dt(dt_range[1]))
        ) for dt_range in expected_ts
    ]

    result = [
        (
            func_name,
            {
                u'$timestamp_begin': datetime_from_ts_us(-args[1]['$timestamp_begin']),
                u'$timestamp_end': datetime_from_ts_us(-args[1]['$timestamp_end']),
                u'$message0': ur'(?i)\Qqwerty\E',
                u'$log_level': 'INFO'
            },
            clean_query(args[0].yql_text)
        ) for func_name, args, kw in calls
    ]

    assert expected[0] == result[0]
    assert expected[1] == result[1]
    assert expected[2] == result[2]
    assert expected[3] == result[3]


def test_tables_generator():
    sc = mock.MagicMock(spec=ydb.SchemeClient)
    sc.list_directory.return_value = make_list_directory(datetime.datetime(2019, 1, 1), 20, forward=True)
    tc = mock.MagicMock(spec=ydb.TableClient)
    reg = metrics.Registry()
    ctl = controller.YdbLogsController(scheme_client=sc,
                                       table_client=tc,
                                       metrics_registry=reg,
                                       history_tables_count=2,
                                       pool_size=1,
                                       request_timeout=1)

    d = datetime.datetime(2019, 1, 6, 23, 59, 59)
    t = timestamp_pb2.Timestamp()
    t.FromDatetime(d)

    gen = ctl.make_tables_generator((None, t), dproxy_pb2.DESC, query_builder=mock.Mock(IN_TABLE_INTERVALS=queries.QueryBuilder.IN_TABLE_INTERVALS), today=d, table_path='/')
    intervals = len(gen.query_builder.IN_TABLE_INTERVALS)

    # DESC
    expected = ['2019-01-04'] + ['2019-01-05'] * intervals + ['2019-01-06'] * intervals
    result = list(item[2].strftime('%Y-%m-%d') for item in gen.cache)
    assert expected == result

    gen = ctl.make_tables_generator((None, t), dproxy_pb2.ASC, query_builder=mock.Mock(IN_TABLE_INTERVALS=queries.QueryBuilder.IN_TABLE_INTERVALS), today=d, table_path='/')

    # ASC
    expected = ['2019-01-06'] * intervals + ['2019-01-05'] * intervals + ['2019-01-04']
    result = list(item[2].strftime('%Y-%m-%d') for item in gen.cache)
    assert expected == result

    sc.list_directory.return_value = make_list_directory(datetime.datetime(2021, 1, 20), 20, forward=True)

    d = datetime.datetime(2021, 1, 26, 9, 50, 13)
    t_begin = timestamp_pb2.Timestamp()
    t_begin.FromDatetime(d)
    d_now = datetime.datetime(2021, 1, 26, 13, 3, 13)
    t_end = timestamp_pb2.Timestamp()
    t_end.FromDatetime(d_now)

    query_builder = mock.Mock(IN_TABLE_INTERVALS=queries.QueryBuilder.IN_TABLE_INTERVALS)
    query_builder.copy.return_value = query_builder
    gen = ctl.make_tables_generator((t_begin, t_end), dproxy_pb2.DESC, query_builder=query_builder, today=d_now, table_path='/')

    expected = [
        (datetime.datetime(2021, 1, 26, 9, 50, 13), datetime.datetime(2021, 1, 26, 12, 3, 13)),
        (datetime.datetime(2021, 1, 26, 12, 3, 13), datetime.datetime(2021, 1, 26, 13, 3, 13)),
    ]
    result = [(item[1], item[2]) for item in gen.cache]
    assert expected == result

    d = datetime.datetime(2021, 1, 26, 3, 50, 13)
    t_begin = timestamp_pb2.Timestamp()
    t_begin.FromDatetime(d)
    d_now = datetime.datetime(2021, 1, 26, 13, 3, 13)

    query_builder = mock.Mock(IN_TABLE_INTERVALS=queries.QueryBuilder.IN_TABLE_INTERVALS)
    query_builder.copy.return_value = query_builder

    gen = ctl.make_tables_generator((t_begin, None), dproxy_pb2.DESC, query_builder=query_builder, table_path='/', now=d_now)

    expected = [
        (datetime.datetime(2021, 1, 26, 3, 50, 13), datetime.datetime(2021, 1, 26, 9, 3, 13)),
        (datetime.datetime(2021, 1, 26, 9, 3, 13), datetime.datetime(2021, 1, 26, 12, 3, 13)),
        (datetime.datetime(2021, 1, 26, 12, 3, 13), datetime.datetime(2021, 1, 26, 13, 3, 13)),
    ]
    result = [(item[1], item[2]) for item in gen.cache]
    assert expected == result

    d_now = datetime.datetime.utcnow()
    sc.list_directory.return_value = make_list_directory(d_now, 20, forward=True)

    d_begin_hours_delta = 4
    d_begin = d_now - datetime.timedelta(hours=d_begin_hours_delta, minutes=15)
    t_begin = timestamp_pb2.Timestamp()
    t_begin.FromDatetime(d_begin)

    gen = ctl.make_tables_generator((t_begin, None), dproxy_pb2.DESC, query_builder=query_builder, table_path='/')

    expected_limit = d_now - datetime.timedelta(hours=d_begin_hours_delta)
    expected_middle_limit = expected_limit + datetime.timedelta(hours=3)

    expected = [
        (start_time.replace(microsecond=0), end_time.replace(microsecond=0))
        for start_time, end_time in
        [
            (d_begin, expected_limit),
            (expected_limit, expected_middle_limit),
            (expected_middle_limit, d_now),
        ]
    ]

    result = [(item[1].replace(microsecond=0), item[2].replace(microsecond=0)) for item in gen.cache]
    assert expected == result


def test_olap_tables_generator():
    sc = mock.MagicMock(spec=ydb.SchemeClient)
    sc.list_directory.return_value = make_olap_list_directory()
    tc = mock.MagicMock(spec=ydb.TableClient)
    reg = metrics.Registry()
    ctl = controller.OlapYdbLogsController(scheme_client=sc,
                                           table_client=tc,
                                           metrics_registry=reg,
                                           history_tables_count=2,
                                           pool_size=1,
                                           request_timeout=1)

    d_end = datetime.datetime(2022, 3, 31, 23, 59, 59)
    t_end = timestamp_pb2.Timestamp()
    t_end.FromDatetime(d_end)

    gen = ctl.make_tables_generator((None, t_end), dproxy_pb2.DESC, query_builder=mock.Mock(), today=d_end, table_path='/')

    # DESC
    expected_min_date, _ = ctl.calc_min_max_possible_dates(today=d_end)
    expected_max_date = d_end

    expected = [(OlapTablesGenerator.TABLE_NAME, expected_min_date, expected_max_date)]
    result = gen.cache
    assert expected == result

    gen = ctl.make_tables_generator((None, t_end), dproxy_pb2.ASC, query_builder=mock.Mock(), today=d_end, table_path='/')

    # ASC
    expected_min_date, _ = ctl.calc_min_max_possible_dates(today=d_end)
    expected_max_date = d_end

    expected = [(OlapTablesGenerator.TABLE_NAME, expected_min_date, expected_max_date)]
    result = gen.cache
    assert expected == result

    d_begin = datetime.datetime(2022, 3, 21, 9, 50, 13)
    t_begin = timestamp_pb2.Timestamp()
    t_begin.FromDatetime(d_begin)

    d_now = datetime.datetime(2022, 3, 31, 13, 3, 13)
    t_end = timestamp_pb2.Timestamp()
    t_end.FromDatetime(d_now)

    query_builder = mock.Mock()
    query_builder.copy.return_value = query_builder
    gen = ctl.make_tables_generator((t_begin, t_end), dproxy_pb2.DESC, query_builder=query_builder, today=d_now, table_path='/')

    expected_min_date = d_begin
    expected_max_date = d_now

    expected = [(OlapTablesGenerator.TABLE_NAME, expected_min_date, expected_max_date)]
    result = gen.cache
    assert expected == result

    d_begin = datetime.datetime(2022, 3, 31, 3, 50, 13)
    t_begin = timestamp_pb2.Timestamp()
    t_begin.FromDatetime(d_begin)
    d_now = datetime.datetime(2022, 3, 31, 13, 3, 13)

    query_builder = mock.Mock()
    query_builder.copy.return_value = query_builder
    gen = ctl.make_tables_generator((t_begin, None), dproxy_pb2.DESC, query_builder=query_builder, table_path='/', now=d_now)

    expected_min_date = d_begin
    expected_max_date = d_now

    expected = [(OlapTablesGenerator.TABLE_NAME, expected_min_date, expected_max_date)]
    result = gen.cache
    assert expected == result

    d_now = datetime.datetime.utcnow()
    d_begin = d_now - datetime.timedelta(hours=4, minutes=15)
    t_begin = timestamp_pb2.Timestamp()
    t_begin.FromDatetime(d_begin)

    query_builder = mock.Mock()
    query_builder.copy.return_value = query_builder
    gen = ctl.make_tables_generator((t_begin, None), dproxy_pb2.DESC, query_builder=query_builder, table_path='/')

    expected_min_date = d_begin.replace(microsecond=0)
    expected_max_date = d_now.replace(microsecond=0)

    expected = [(OlapTablesGenerator.TABLE_NAME, expected_min_date, expected_max_date)]

    result = gen.cache

    for i, item in enumerate(result):
        name, min_date, max_date = item
        result[i] = (name, min_date.replace(microsecond=0), max_date.replace(microsecond=0))

    assert expected == result


def test_user_fields():
    expected_q = clean_query(
        """
        PRAGMA TablePathPrefix("/foo/bar");
        PRAGMA AnsiInForEmptyOrNullableItemsCollections;
        DECLARE $context0_values AS List<Utf8>;
        DECLARE $context1_values AS List<Utf8>;
        DECLARE $timestamp_begin AS Int64;
        DECLARE $timestamp_end AS Int64;
        SELECT `timestamp`, `container_id`, `host`, `pod`, `box`, `workload`,
               `logger_name`, `user_id`, `request_id`, `message`, `log_level`, `seq`, `context`,
               `pod_transient_fqdn`, `pod_persistent_fqdn`, `node_fqdn`, `stack_trace`, `thread_name`,
               `log_level_int`
        FROM `{ts_date}`
        WHERE JSON_VALUE(CAST(`context` as Json), "lax $.'a'.'b'" DEFAULT "" ON EMPTY DEFAULT "" ON ERROR) IN COMPACT $context0_values
              AND JSON_VALUE(CAST(`context` as Json), "lax $.'f'.'g'" DEFAULT "" ON EMPTY DEFAULT "" ON ERROR) NOT IN COMPACT $context1_values
              AND `timestamp` <= $timestamp_begin
              AND `timestamp` >= $timestamp_end
        ORDER BY `timestamp` ASC, `container_id` ASC, `seq` ASC
        LIMIT 3;
        """.format(ts_date=DateTablesGenerator.make_table_name_from_dt(TS_DATE))
    )

    rows = []
    rows.append(make_row(TS))
    rows.append(make_row(TS - 1))
    rows.append(make_row(TS - 2))

    sc = mock.MagicMock(spec=ydb.SchemeClient)
    sc.list_directory.return_value = make_list_directory(TS_DATE, 10)
    tc = mock.MagicMock(spec=ydb.TableClient)
    tc.scan_query.return_value = [mock.Mock(result_set=mock.Mock(rows=rows))]

    reg = metrics.Registry()
    ctl = controller.YdbLogsController(scheme_client=sc,
                                       table_client=tc,
                                       metrics_registry=reg,
                                       history_tables_count=100,
                                       pool_size=1,
                                       request_timeout=1)

    from_ts = timestamp_pb2.Timestamp()
    from_ts.FromMicroseconds(TS + 1)

    resp = ctl.search(
        table_path='/foo/bar',
        continuation_token=None,
        timestamp_range=(None, from_ts),
        search_patterns=None,
        log_levels=None,
        log_levels_int=None,
        hosts=None,
        pods=None,
        boxes=None,
        workloads=None,
        containers=None,
        logger_names=None,
        pod_transient_fqdns=None,
        pod_persistent_fqdns=None,
        node_fqdns=None,
        thread_names=None,
        stack_traces=None,
        request_ids=None,
        user_fields=[
            (['a', 'b'], ['d', 'e'], INCLUDE),
            (['f', 'g'], ['h'], EXCLUDE),
        ],
        limit=3,
        order=dproxy_pb2.DESC,
    )

    assert_ts(resp, rows)

    calls = tc.method_calls
    assert len(calls) == 1
    n, args, kw = calls[0]
    assert n == 'scan_query'
    assert clean_query(args[0].yql_text) == expected_q

    args[1][u'$timestamp_begin'] = datetime_from_ts_us(-args[1][u'$timestamp_begin'])
    args[1][u'$timestamp_end'] = datetime_from_ts_us(-args[1][u'$timestamp_end'])
    dt = datetime_from_ts_us(TS + 1)
    assert args[1] == {
        u'$context0_values': ['d', 'e'],
        u'$context1_values': ['h'],
        u'$timestamp_end': dt,
        u'$timestamp_begin': max(dt - datetime.timedelta(hours=1), start_of_day(dt)),
    }


def test_olap_user_fields():
    rows = []
    rows.append(olap_make_row(TS))
    rows.append(olap_make_row(TS - 1))
    rows.append(olap_make_row(TS - 2))

    sc = mock.MagicMock(spec=ydb.SchemeClient)
    sc.list_directory.return_value = make_olap_list_directory()
    tc = mock.MagicMock(spec=ydb.TableClient)
    tc.scan_query.return_value = [mock.Mock(result_set=mock.Mock(rows=rows))]

    reg = metrics.Registry()
    ctl = controller.OlapYdbLogsController(scheme_client=sc,
                                           table_client=tc,
                                           metrics_registry=reg,
                                           history_tables_count=100,
                                           pool_size=1,
                                           request_timeout=1)

    end_ms = TS + 1
    end_dt = microseconds_to_dt(end_ms)
    begin_dt, _ = ctl.calc_min_max_possible_dates()
    order_pb = dproxy_pb2.DESC

    expected_q = clean_query(
        """
        PRAGMA Kikimr.EnableLlvm="false";
        PRAGMA Kikimr.KqpPushOlapProcess="true";
        PRAGMA TablePathPrefix("/foo/bar");
        PRAGMA AnsiInForEmptyOrNullableItemsCollections;
        DECLARE $context0_values AS List<Utf8>;
        DECLARE $context1_values AS List<Utf8>;
        $timestamp_begin = CAST("{ts_begin_str}" AS Timestamp);
        $timestamp_end = CAST("{ts_end_str}" AS Timestamp);
        SELECT `timestamp`, `container_id`, `host`, `pod`, `box`, `workload`,
               `logger_name`, `user_id`, `request_id`, `message`, `log_level`, `seq`, `context`,
               `pod_transient_fqdn`, `pod_persistent_fqdn`, `node_fqdn`, `stack_trace`, `thread_name`,
               `log_level_int`
        FROM `{olap_table_name}`
        WHERE JSON_VALUE(CAST(`context` as Json), "lax $.'a'.'b'" DEFAULT "" ON EMPTY DEFAULT "" ON ERROR) IN COMPACT $context0_values
              AND JSON_VALUE(CAST(`context` as Json), "lax $.'f'.'g'" DEFAULT "" ON EMPTY DEFAULT "" ON ERROR) NOT IN COMPACT $context1_values
              AND $timestamp_begin <= `timestamp`
              AND `timestamp` <= $timestamp_end
        ORDER BY `timestamp` {order}, `host` {order}, `seq` {order}
        LIMIT 3;
        """.format(
            ts_begin_str=dt_to_str(begin_dt),
            ts_end_str=dt_to_str(end_dt),
            olap_table_name=OlapTablesGenerator.TABLE_NAME,
            order=queries.ORDER_PB_TO_STR[order_pb],
        )
    )

    from_ts = timestamp_pb2.Timestamp()
    from_ts.FromMicroseconds(end_ms)

    resp = ctl.search(
        table_path='/foo/bar',
        continuation_token=None,
        timestamp_range=(None, from_ts),
        search_patterns=None,
        log_levels=None,
        log_levels_int=None,
        hosts=None,
        pods=None,
        boxes=None,
        workloads=None,
        containers=None,
        logger_names=None,
        pod_transient_fqdns=None,
        pod_persistent_fqdns=None,
        node_fqdns=None,
        thread_names=None,
        stack_traces=None,
        request_ids=None,
        user_fields=[
            (['a', 'b'], ['d', 'e'], INCLUDE),
            (['f', 'g'], ['h'], EXCLUDE),
        ],
        limit=3,
        order=order_pb,
    )

    assert_ts(resp, rows, ts_converter=ts_to_ydb_timestamp)

    calls = tc.method_calls
    assert len(calls) == 1
    n, args, kw = calls[0]
    assert n == 'scan_query'
    assert clean_query(args[0].yql_text) == expected_q

    assert args[1] == {
        u'$context0_values': ['d', 'e'],
        u'$context1_values': ['h'],
    }


def test_continuation():
    expected_q = clean_query(
        """
        PRAGMA TablePathPrefix("/foo/bar");
        PRAGMA AnsiInForEmptyOrNullableItemsCollections;
        DECLARE $log_level AS List<Utf8>;
        DECLARE $message0 AS Utf8;
        DECLARE $message1 AS Utf8;
        DECLARE $host AS List<Utf8>;
        DECLARE $pod AS List<Utf8>;
        DECLARE $box AS List<Utf8>;
        DECLARE $workload AS List<Utf8>;
        DECLARE $container_id AS List<Utf8>;
        DECLARE $logger_name AS List<Utf8>;
        DECLARE $timestamp_begin AS Int64;
        DECLARE $timestamp_end AS Int64;
        $message0_re = Hyperscan::Grep($message0);
        $message1_re = Hyperscan::Grep($message1);
        SELECT `timestamp`, `container_id`, `host`, `pod`, `box`, `workload`,
               `logger_name`, `user_id`, `request_id`, `message`, `log_level`, `seq`, `context`,
               `pod_transient_fqdn`, `pod_persistent_fqdn`, `node_fqdn`, `stack_trace`, `thread_name`,
               `log_level_int`
        FROM `{ts_date}`
        WHERE `log_level` IN COMPACT $log_level
              AND ($message0_re(`message`) OR $message1_re(`message`))
              AND `host` NOT IN COMPACT $host
              AND `pod` IN COMPACT $pod
              AND `box` IN COMPACT $box
              AND `workload` IN COMPACT $workload
              AND `container_id` IN COMPACT $container_id
              AND `logger_name` IN COMPACT $logger_name
              AND `timestamp` <= $timestamp_begin
              AND `timestamp` >= $timestamp_end
        ORDER BY `timestamp` ASC, `container_id` ASC, `seq` ASC
        LIMIT 3;
        """.format(ts_date=DateTablesGenerator.make_table_name_from_dt(TS_DATE))
    )
    rows = []
    rows.append(make_row(TS))
    rows.append(make_row(TS - 1))
    rows.append(make_row(TS - 2))

    sc = mock.MagicMock(spec=ydb.SchemeClient)
    sc.list_directory.return_value = make_list_directory(TS_DATE, 10)
    tc = mock.MagicMock(spec=ydb.TableClient)
    tc.scan_query.return_value = [mock.Mock(result_set=mock.Mock(rows=rows))]

    reg = metrics.Registry()
    ctl = controller.YdbLogsController(scheme_client=sc,
                                       table_client=tc,
                                       metrics_registry=reg,
                                       history_tables_count=100,
                                       pool_size=1,
                                       request_timeout=1)

    from_ts = timestamp_pb2.Timestamp()
    from_ts.FromMicroseconds(TS + 1)
    to_ts = timestamp_pb2.Timestamp()
    to_ts.FromMicroseconds(TS + 100)

    kwargs = dict(
        search_patterns=(['qwerty1', 'qwerty2'], INCLUDE),
        log_levels=(['INFO', 'DEBUG'], INCLUDE),
        log_levels_int=None,
        hosts=(['host1', 'host2'], EXCLUDE),
        pods=(['pod1', 'pod2'], INCLUDE),
        boxes=(['box1', 'box2'], INCLUDE),
        workloads=(['wl1', 'wl2'], INCLUDE),
        containers=(['cnt1', 'cnt2'], INCLUDE),
        logger_names=(['log1', 'log2'], INCLUDE),
        pod_transient_fqdns=None,
        pod_persistent_fqdns=None,
        node_fqdns=None,
        thread_names=None,
        request_ids=None,
        stack_traces=None,
        user_fields=None,
    )
    resp = ctl.search(
        table_path='/foo/bar',
        continuation_token=None,
        timestamp_range=(from_ts, to_ts),
        limit=3,
        order=dproxy_pb2.DESC,
        **kwargs
    )

    assert_ts(resp, rows)

    calls = tc.method_calls
    assert len(calls) == 1
    n, args, kw = calls[0]
    assert n == 'scan_query'
    assert clean_query(args[0].yql_text) == expected_q
    assert args[1] == {
        '$timestamp_begin': -(TS + 1),
        '$timestamp_end': -(TS + 100),
        '$message0': ur'(?i)\Qqwerty1\E',
        '$message1': ur'(?i)\Qqwerty2\E',
        '$log_level': ['INFO', 'DEBUG'],
        '$host': ['host1', 'host2'],
        '$pod': ['pod1', 'pod2'],
        '$box': ['box1', 'box2'],
        '$workload': ['wl1', 'wl2'],
        '$container_id': ['cnt1', 'cnt2'],
        '$logger_name': ['log1', 'log2'],
    }

    forward_token = resp.continuation_tokens.forward
    backward_token = resp.continuation_tokens.backward
    current_token = resp.continuation_tokens.current
    assert forward_token
    assert backward_token
    assert current_token

    expected_q = clean_query(
        """
        PRAGMA TablePathPrefix("/foo/bar");
        PRAGMA AnsiInForEmptyOrNullableItemsCollections;
        DECLARE $log_level AS List<Utf8>;
        DECLARE $message0 AS Utf8;
        DECLARE $message1 AS Utf8;
        DECLARE $host AS List<Utf8>;
        DECLARE $pod AS List<Utf8>;
        DECLARE $box AS List<Utf8>;
        DECLARE $workload AS List<Utf8>;
        DECLARE $container_id AS List<Utf8>;
        DECLARE $logger_name AS List<Utf8>;
        DECLARE $continuation_timestamp AS Int64;
        DECLARE $continuation_seq AS Uint64;
        DECLARE $continuation_container AS Utf8;
        DECLARE $timestamp_begin AS Int64;
        $message0_re = Hyperscan::Grep($message0);
        $message1_re = Hyperscan::Grep($message1);
        $q1 = (
        SELECT `timestamp`, `container_id`, `host`, `pod`, `box`, `workload`,
            `logger_name`, `user_id`, `request_id`, `message`,
            `log_level`, `seq`, `context`, `pod_transient_fqdn`, `pod_persistent_fqdn`,
            `node_fqdn`, `stack_trace`, `thread_name`, `log_level_int`
        FROM `{ts_date}`
        WHERE
            `timestamp` = $continuation_timestamp
            AND `container_id` = $continuation_container
            AND `seq` > $continuation_seq
            AND `log_level` IN COMPACT $log_level
            AND ($message0_re(`message`) OR $message1_re(`message`))
            AND `host` NOT IN COMPACT $host
            AND `pod` IN COMPACT $pod
            AND `box` IN COMPACT $box
            AND `workload` IN COMPACT $workload
            AND `container_id` IN COMPACT $container_id
            AND `logger_name` IN COMPACT $logger_name
            AND `timestamp` <= $timestamp_begin
        ORDER BY `timestamp` ASC, `container_id` ASC, `seq` ASC
        LIMIT 3
        );
        $q2 = (
        SELECT `timestamp`, `container_id`, `host`, `pod`, `box`, `workload`,
            `logger_name`, `user_id`, `request_id`, `message`,
            `log_level`, `seq`, `context`, `pod_transient_fqdn`, `pod_persistent_fqdn`,
            `node_fqdn`, `stack_trace`, `thread_name`, `log_level_int`
        FROM `{ts_date}`
        WHERE
            `timestamp` = $continuation_timestamp
            AND `container_id` > $continuation_container
            AND `log_level` IN COMPACT $log_level
            AND ($message0_re(`message`) OR $message1_re(`message`))
            AND `host` NOT IN COMPACT $host AND `pod` IN COMPACT $pod
            AND `box` IN COMPACT $box
            AND `workload` IN COMPACT $workload
            AND `container_id` IN COMPACT $container_id
            AND `logger_name` IN COMPACT $logger_name
            AND `timestamp` <= $timestamp_begin
        ORDER BY `timestamp` ASC, `container_id` ASC, `seq` ASC
        LIMIT 3
        );
        $q3 = (
        SELECT `timestamp`, `container_id`, `host`, `pod`, `box`, `workload`,
            `logger_name`, `user_id`, `request_id`, `message`,
            `log_level`, `seq`, `context`, `pod_transient_fqdn`, `pod_persistent_fqdn`,
            `node_fqdn`, `stack_trace`, `thread_name`, `log_level_int`
        FROM `{ts_date}`
        WHERE
            `timestamp` > $continuation_timestamp
            AND `log_level` IN COMPACT $log_level
            AND ($message0_re(`message`) OR $message1_re(`message`))
            AND `host` NOT IN COMPACT $host
            AND `pod` IN COMPACT $pod
            AND `box` IN COMPACT $box
            AND `workload` IN COMPACT $workload
            AND `container_id` IN COMPACT $container_id
            AND `logger_name` IN COMPACT $logger_name
            AND `timestamp` <= $timestamp_begin
        ORDER BY `timestamp` ASC, `container_id` ASC, `seq` ASC
        LIMIT 3
        );
        $q4 = (SELECT * FROM $q1 UNION ALL SELECT * FROM $q2 UNION ALL SELECT * FROM $q3);
        SELECT * from $q4
        ORDER BY `timestamp` ASC, `container_id` ASC, `seq` ASC
        LIMIT 3
        """.format(ts_date=DateTablesGenerator.make_table_name_from_dt(TS_DATE))
    )

    resp = ctl.search(
        table_path='/foo/bar',
        timestamp_range=(from_ts, to_ts),
        limit=3,
        order=dproxy_pb2.DESC,
        continuation_token=forward_token,
        **kwargs
    )

    assert_ts(resp, rows)

    calls = tc.method_calls
    assert len(calls) == 2
    n, args, kw = calls[1]
    assert n == 'scan_query'
    assert clean_query(args[0].yql_text) == expected_q
    assert args[1] == {
        u'$timestamp_begin': -(TS + 1),
        u'$message0': ur'(?i)\Qqwerty1\E',
        u'$message1': ur'(?i)\Qqwerty2\E',
        u'$log_level': ['INFO', 'DEBUG'],
        u'$host': ['host1', 'host2'],
        u'$pod': ['pod1', 'pod2'],
        u'$box': ['box1', 'box2'],
        u'$workload': ['wl1', 'wl2'],
        u'$container_id': ['cnt1', 'cnt2'],
        u'$logger_name': ['log1', 'log2'],
        u'$continuation_timestamp': rows[-1]['timestamp'],
        u'$continuation_container': rows[-1]['container_id'],
        u'$continuation_seq': rows[-1]['seq'],
    }

    expected_q = clean_query(
        """
        PRAGMA TablePathPrefix("/foo/bar");
        PRAGMA AnsiInForEmptyOrNullableItemsCollections;
        DECLARE $log_level AS List<Utf8>;
        DECLARE $message0 AS Utf8;
        DECLARE $message1 AS Utf8;
        DECLARE $host AS List<Utf8>;
        DECLARE $pod AS List<Utf8>;
        DECLARE $box AS List<Utf8>;
        DECLARE $workload AS List<Utf8>;
        DECLARE $container_id AS List<Utf8>;
        DECLARE $logger_name AS List<Utf8>;
        DECLARE $continuation_timestamp AS Int64;
        DECLARE $continuation_seq AS Uint64;
        DECLARE $continuation_container AS Utf8;
        DECLARE $timestamp_end AS Int64;
        $message0_re = Hyperscan::Grep($message0);
        $message1_re = Hyperscan::Grep($message1);
        $q1 = (
        SELECT `timestamp`, `container_id`, `host`, `pod`, `box`, `workload`,
            `logger_name`, `user_id`, `request_id`, `message`,
            `log_level`, `seq`, `context`, `pod_transient_fqdn`, `pod_persistent_fqdn`,
            `node_fqdn`, `stack_trace`, `thread_name`, `log_level_int`
        FROM `{ts_date}`
        WHERE
            `timestamp` = $continuation_timestamp
            AND `container_id` = $continuation_container
            AND `seq` <= $continuation_seq
            AND `log_level` IN COMPACT $log_level
            AND ($message0_re(`message`) OR $message1_re(`message`))
            AND `host` NOT IN COMPACT $host
            AND `pod` IN COMPACT $pod
            AND `box` IN COMPACT $box
            AND `workload` IN COMPACT $workload
            AND `container_id` IN COMPACT $container_id
            AND `logger_name` IN COMPACT $logger_name
            AND `timestamp` >= $timestamp_end
        ORDER BY `timestamp` ASC, `container_id` ASC, `seq` ASC
        LIMIT 3
        );
        $q2 = (
        SELECT `timestamp`, `container_id`, `host`, `pod`, `box`, `workload`,
            `logger_name`, `user_id`, `request_id`, `message`,
            `log_level`, `seq`, `context`, `pod_transient_fqdn`, `pod_persistent_fqdn`,
            `node_fqdn`, `stack_trace`, `thread_name`, `log_level_int`
        FROM `{ts_date}`
        WHERE
            `timestamp` = $continuation_timestamp
            AND `container_id` < $continuation_container
            AND `log_level` IN COMPACT $log_level
            AND ($message0_re(`message`) OR $message1_re(`message`))
            AND `host` NOT IN COMPACT $host
            AND `pod` IN COMPACT $pod
            AND `box` IN COMPACT $box
            AND `workload` IN COMPACT $workload
            AND `container_id` IN COMPACT $container_id
            AND `logger_name` IN COMPACT $logger_name
            AND `timestamp` >= $timestamp_end
        ORDER BY `timestamp` ASC, `container_id` ASC, `seq` ASC
        LIMIT 3
        );
        $q3 = (
        SELECT `timestamp`, `container_id`, `host`, `pod`, `box`, `workload`,
            `logger_name`, `user_id`, `request_id`, `message`,
            `log_level`, `seq`, `context`, `pod_transient_fqdn`, `pod_persistent_fqdn`,
            `node_fqdn`, `stack_trace`, `thread_name`, `log_level_int`
        FROM `{ts_date}`
        WHERE
            `timestamp` < $continuation_timestamp
            AND `log_level` IN COMPACT $log_level
            AND ($message0_re(`message`) OR $message1_re(`message`))
            AND `host` NOT IN COMPACT $host
            AND `pod` IN COMPACT $pod
            AND `box` IN COMPACT $box
            AND `workload` IN COMPACT $workload
            AND `container_id` IN COMPACT $container_id
            AND `logger_name` IN COMPACT $logger_name
            AND `timestamp` >= $timestamp_end
        ORDER BY `timestamp` ASC, `container_id` ASC, `seq` ASC
        LIMIT 3
        );
        $q4 = (SELECT * FROM $q1 UNION ALL SELECT * FROM $q2 UNION ALL SELECT * FROM $q3);
        SELECT * from $q4
        ORDER BY `timestamp` ASC, `container_id` ASC, `seq` ASC
        LIMIT 3
        """.format(ts_date=DateTablesGenerator.make_table_name_from_dt(TS_DATE))
    )

    resp = ctl.search(
        table_path='/foo/bar',
        timestamp_range=(from_ts, to_ts),
        limit=3,
        order=dproxy_pb2.DESC,
        continuation_token=backward_token,
        **kwargs
    )

    assert_ts(resp, rows)

    calls = tc.method_calls
    assert len(calls) == 3
    n, args, kw = calls[2]
    assert n == 'scan_query'
    assert clean_query(args[0].yql_text) == expected_q
    assert args[1] == {
        u'$timestamp_end': -(TS + 100),
        u'$message0': ur'(?i)\Qqwerty1\E',
        u'$message1': ur'(?i)\Qqwerty2\E',
        u'$log_level': ['INFO', 'DEBUG'],
        u'$host': ['host1', 'host2'],
        u'$pod': ['pod1', 'pod2'],
        u'$box': ['box1', 'box2'],
        u'$workload': ['wl1', 'wl2'],
        u'$container_id': ['cnt1', 'cnt2'],
        u'$logger_name': ['log1', 'log2'],
        u'$continuation_timestamp': -(TS + 1),
        u'$continuation_container': rows[0]['container_id'],
        u'$continuation_seq': rows[0]['seq'],
    }

    expected_q = clean_query(
        """
        PRAGMA TablePathPrefix("/foo/bar");
        PRAGMA AnsiInForEmptyOrNullableItemsCollections;
        DECLARE $log_level AS List<Utf8>;
        DECLARE $message0 AS Utf8;
        DECLARE $message1 AS Utf8;
        DECLARE $host AS List<Utf8>;
        DECLARE $pod AS List<Utf8>;
        DECLARE $box AS List<Utf8>;
        DECLARE $workload AS List<Utf8>;
        DECLARE $container_id AS List<Utf8>;
        DECLARE $logger_name AS List<Utf8>;
        DECLARE $continuation_timestamp AS Int64;
        DECLARE $continuation_seq AS Uint64;
        DECLARE $continuation_container AS Utf8;
        DECLARE $timestamp_begin AS Int64;
        $message0_re = Hyperscan::Grep($message0);
        $message1_re = Hyperscan::Grep($message1);
        $q1 = (
        SELECT `timestamp`, `container_id`, `host`, `pod`, `box`, `workload`,
            `logger_name`, `user_id`, `request_id`, `message`,
            `log_level`, `seq`, `context`, `pod_transient_fqdn`, `pod_persistent_fqdn`,
            `node_fqdn`, `stack_trace`, `thread_name`, `log_level_int`
        FROM `{ts_date}`
        WHERE
            `timestamp` = $continuation_timestamp
            AND `container_id` = $continuation_container
            AND `seq` >= $continuation_seq
            AND `log_level` IN COMPACT $log_level
            AND ($message0_re(`message`) OR $message1_re(`message`))
            AND `host` NOT IN COMPACT $host
            AND `pod` IN COMPACT $pod
            AND `box` IN COMPACT $box
            AND `workload` IN COMPACT $workload
            AND `container_id` IN COMPACT $container_id
            AND `logger_name` IN COMPACT $logger_name
            AND `timestamp` <= $timestamp_begin
        ORDER BY `timestamp` ASC, `container_id` ASC, `seq` ASC
        LIMIT 3
        );
        $q2 = (
        SELECT `timestamp`, `container_id`, `host`, `pod`, `box`, `workload`,
            `logger_name`, `user_id`, `request_id`, `message`,
            `log_level`, `seq`, `context`, `pod_transient_fqdn`, `pod_persistent_fqdn`,
            `node_fqdn`, `stack_trace`, `thread_name`, `log_level_int`
        FROM `{ts_date}`
        WHERE
            `timestamp` = $continuation_timestamp
            AND `container_id` > $continuation_container
            AND `log_level` IN COMPACT $log_level
            AND ($message0_re(`message`) OR $message1_re(`message`))
            AND `host` NOT IN COMPACT $host
            AND `pod` IN COMPACT $pod
            AND `box` IN COMPACT $box
            AND `workload` IN COMPACT $workload
            AND `container_id` IN COMPACT $container_id
            AND `logger_name` IN COMPACT $logger_name
            AND `timestamp` <= $timestamp_begin
        ORDER BY `timestamp` ASC, `container_id` ASC, `seq` ASC
        LIMIT 3
        );
        $q3 = (
        SELECT `timestamp`, `container_id`, `host`, `pod`, `box`, `workload`,
            `logger_name`, `user_id`, `request_id`, `message`,
            `log_level`, `seq`, `context`, `pod_transient_fqdn`, `pod_persistent_fqdn`,
            `node_fqdn`, `stack_trace`, `thread_name`, `log_level_int`
        FROM `{ts_date}`
        WHERE
            `timestamp` > $continuation_timestamp
            AND `log_level` IN COMPACT $log_level
            AND ($message0_re(`message`) OR $message1_re(`message`))
            AND `host` NOT IN COMPACT $host
            AND `pod` IN COMPACT $pod
            AND `box` IN COMPACT $box
            AND `workload` IN COMPACT $workload
            AND `container_id` IN COMPACT $container_id
            AND `logger_name` IN COMPACT $logger_name
            AND `timestamp` <= $timestamp_begin
        ORDER BY `timestamp` ASC, `container_id` ASC, `seq` ASC
        LIMIT 3
        );
        $q4 = (SELECT * FROM $q1 UNION ALL SELECT * FROM $q2 UNION ALL SELECT * FROM $q3);
        SELECT * from $q4
        ORDER BY `timestamp` ASC, `container_id` ASC, `seq` ASC
        LIMIT 3
        """.format(ts_date=DateTablesGenerator.make_table_name_from_dt(TS_DATE))
    )

    resp = ctl.search(
        table_path='/foo/bar',
        timestamp_range=(from_ts, to_ts),
        limit=3,
        order=dproxy_pb2.DESC,
        continuation_token=current_token,
        **kwargs
    )

    assert_ts(resp, rows)

    calls = tc.method_calls
    assert len(calls) == 4
    n, args, kw = calls[3]
    assert n == 'scan_query'
    assert clean_query(args[0].yql_text) == expected_q
    assert args[1] == {
        u'$timestamp_begin': -(TS + 1),
        u'$message0': ur'(?i)\Qqwerty1\E',
        u'$message1': ur'(?i)\Qqwerty2\E',
        u'$log_level': ['INFO', 'DEBUG'],
        u'$host': ['host1', 'host2'],
        u'$pod': ['pod1', 'pod2'],
        u'$box': ['box1', 'box2'],
        u'$workload': ['wl1', 'wl2'],
        u'$container_id': ['cnt1', 'cnt2'],
        u'$logger_name': ['log1', 'log2'],
        u'$continuation_timestamp': rows[0]['timestamp'],
        u'$continuation_container': rows[0]['container_id'],
        u'$continuation_seq': rows[0]['seq'],
    }


def test_olap_continuation():
    rows = []
    rows.append(olap_make_row(TS))
    rows.append(olap_make_row(TS - 1))
    rows.append(olap_make_row(TS - 2))

    sc = mock.MagicMock(spec=ydb.SchemeClient)
    sc.list_directory.return_value = make_olap_list_directory()
    tc = mock.MagicMock(spec=ydb.TableClient)
    tc.scan_query.return_value = [mock.Mock(result_set=mock.Mock(rows=rows))]

    reg = metrics.Registry()
    ctl = controller.OlapYdbLogsController(scheme_client=sc,
                                           table_client=tc,
                                           metrics_registry=reg,
                                           history_tables_count=100,
                                           pool_size=1,
                                           request_timeout=1)

    begin_ms = TS + 1
    begin_dt = microseconds_to_dt(begin_ms)

    end_ms = TS + 100
    end_dt = microseconds_to_dt(end_ms)

    order_pb = dproxy_pb2.DESC

    expected_q = clean_query(
        """
        PRAGMA Kikimr.EnableLlvm="false";
        PRAGMA Kikimr.KqpPushOlapProcess="true";
        PRAGMA TablePathPrefix("/foo/bar");
        PRAGMA AnsiInForEmptyOrNullableItemsCollections;
        DECLARE $log_level AS List<Utf8>;
        DECLARE $message0 AS Utf8;
        DECLARE $message1 AS Utf8;
        DECLARE $host AS List<Utf8>;
        DECLARE $pod AS List<Utf8>;
        DECLARE $box AS List<Utf8>;
        DECLARE $workload AS List<Utf8>;
        DECLARE $container_id AS List<Utf8>;
        DECLARE $logger_name AS List<Utf8>;
        $message0_re = Hyperscan::Grep($message0);
        $message1_re = Hyperscan::Grep($message1);
        $timestamp_begin = CAST("{ts_begin_str}" AS Timestamp);
        $timestamp_end = CAST("{ts_end_str}" AS Timestamp);
        SELECT `timestamp`, `container_id`, `host`, `pod`, `box`, `workload`,
               `logger_name`, `user_id`, `request_id`, `message`, `log_level`, `seq`, `context`,
               `pod_transient_fqdn`, `pod_persistent_fqdn`, `node_fqdn`, `stack_trace`, `thread_name`,
               `log_level_int`
        FROM `{olap_table_name}`
        WHERE `log_level` IN COMPACT $log_level
              AND ($message0_re(`message`) OR $message1_re(`message`))
              AND `host` NOT IN COMPACT $host
              AND `pod` IN COMPACT $pod
              AND `box` IN COMPACT $box
              AND `workload` IN COMPACT $workload
              AND `container_id` IN COMPACT $container_id
              AND `logger_name` IN COMPACT $logger_name
              AND $timestamp_begin <= `timestamp`
              AND `timestamp` <= $timestamp_end
        ORDER BY `timestamp` {order}, `host` {order}, `seq` {order}
        LIMIT 3;
        """.format(
            ts_begin_str=dt_to_str(begin_dt),
            ts_end_str=dt_to_str(end_dt),
            olap_table_name=OlapTablesGenerator.TABLE_NAME,
            order=queries.ORDER_PB_TO_STR[order_pb],
        )
    )

    from_ts = timestamp_pb2.Timestamp()
    from_ts.FromMicroseconds(begin_ms)
    to_ts = timestamp_pb2.Timestamp()
    to_ts.FromMicroseconds(end_ms)

    kwargs = dict(
        search_patterns=(['qwerty1', 'qwerty2'], INCLUDE),
        log_levels=(['INFO', 'DEBUG'], INCLUDE),
        log_levels_int=None,
        hosts=(['host1', 'host2'], EXCLUDE),
        pods=(['pod1', 'pod2'], INCLUDE),
        boxes=(['box1', 'box2'], INCLUDE),
        workloads=(['wl1', 'wl2'], INCLUDE),
        containers=(['cnt1', 'cnt2'], INCLUDE),
        logger_names=(['log1', 'log2'], INCLUDE),
        pod_transient_fqdns=None,
        pod_persistent_fqdns=None,
        node_fqdns=None,
        thread_names=None,
        request_ids=None,
        stack_traces=None,
        user_fields=None,
    )
    resp = ctl.search(
        table_path='/foo/bar',
        continuation_token=None,
        timestamp_range=(from_ts, to_ts),
        limit=3,
        order=order_pb,
        **kwargs
    )

    assert_ts(resp, rows, ts_converter=ts_to_ydb_timestamp)

    calls = tc.method_calls
    assert len(calls) == 1
    n, args, kw = calls[0]
    assert n == 'scan_query'
    assert clean_query(args[0].yql_text) == expected_q
    assert args[1] == {
        '$message0': ur'(?i)\Qqwerty1\E',
        '$message1': ur'(?i)\Qqwerty2\E',
        '$log_level': ['INFO', 'DEBUG'],
        '$host': ['host1', 'host2'],
        '$pod': ['pod1', 'pod2'],
        '$box': ['box1', 'box2'],
        '$workload': ['wl1', 'wl2'],
        '$container_id': ['cnt1', 'cnt2'],
        '$logger_name': ['log1', 'log2'],
    }

    forward_token = resp.continuation_tokens.forward
    backward_token = resp.continuation_tokens.backward
    current_token = resp.continuation_tokens.current
    assert forward_token
    assert backward_token
    assert current_token

    continuation_dt = microseconds_to_dt(rows[-1][queries.OlapQueryBuilder.TIMESTAMP_FLD])

    expected_q = clean_query(
        """
        PRAGMA Kikimr.EnableLlvm="false";
        PRAGMA Kikimr.KqpPushOlapProcess="true";
        PRAGMA TablePathPrefix("/foo/bar");
        PRAGMA AnsiInForEmptyOrNullableItemsCollections;
        DECLARE $log_level AS List<Utf8>;
        DECLARE $message0 AS Utf8;
        DECLARE $message1 AS Utf8;
        DECLARE $host AS List<Utf8>;
        DECLARE $pod AS List<Utf8>;
        DECLARE $box AS List<Utf8>;
        DECLARE $workload AS List<Utf8>;
        DECLARE $container_id AS List<Utf8>;
        DECLARE $logger_name AS List<Utf8>;
        DECLARE $continuation_seq AS Uint64;
        DECLARE $continuation_host AS Utf8;
        $message0_re = Hyperscan::Grep($message0);
        $message1_re = Hyperscan::Grep($message1);
        $timestamp_begin = CAST("{ts_begin_str}" AS Timestamp);
        $continuation_timestamp = CAST("{ts_continuation_str}" AS Timestamp);
        SELECT `timestamp`, `container_id`, `host`, `pod`, `box`, `workload`,
            `logger_name`, `user_id`, `request_id`, `message`,
            `log_level`, `seq`, `context`, `pod_transient_fqdn`, `pod_persistent_fqdn`,
            `node_fqdn`, `stack_trace`, `thread_name`, `log_level_int`
        FROM `{olap_table_name}`
        WHERE (`timestamp`, `host`, `seq`) < ($continuation_timestamp, $continuation_host, $continuation_seq)
            AND `log_level` IN COMPACT $log_level
            AND ($message0_re(`message`) OR $message1_re(`message`))
            AND `host` NOT IN COMPACT $host
            AND `pod` IN COMPACT $pod
            AND `box` IN COMPACT $box
            AND `workload` IN COMPACT $workload
            AND `container_id` IN COMPACT $container_id
            AND `logger_name` IN COMPACT $logger_name
            AND $timestamp_begin <= `timestamp`
        ORDER BY `timestamp` {order}, `host` {order}, `seq` {order}
        LIMIT 3
        """.format(
            ts_begin_str=dt_to_str(begin_dt),
            ts_continuation_str=dt_to_str(continuation_dt),
            olap_table_name=OlapTablesGenerator.TABLE_NAME,
            order=queries.ORDER_PB_TO_STR[order_pb],
        )
    )

    resp = ctl.search(
        table_path='/foo/bar',
        timestamp_range=(from_ts, to_ts),
        limit=3,
        order=order_pb,
        continuation_token=forward_token,
        **kwargs
    )

    assert_ts(resp, rows, ts_converter=ts_to_ydb_timestamp)

    calls = tc.method_calls
    assert len(calls) == 2
    n, args, kw = calls[1]
    assert n == 'scan_query'
    assert clean_query(args[0].yql_text) == expected_q
    assert args[1] == {
        u'$message0': ur'(?i)\Qqwerty1\E',
        u'$message1': ur'(?i)\Qqwerty2\E',
        u'$log_level': ['INFO', 'DEBUG'],
        u'$host': ['host1', 'host2'],
        u'$pod': ['pod1', 'pod2'],
        u'$box': ['box1', 'box2'],
        u'$workload': ['wl1', 'wl2'],
        u'$container_id': ['cnt1', 'cnt2'],
        u'$logger_name': ['log1', 'log2'],
        u'$continuation_host': rows[-1]['host'],
        u'$continuation_seq': rows[-1]['seq'],
    }

    continuation_dt = microseconds_to_dt(begin_ms)

    expected_q = clean_query(
        """
        PRAGMA Kikimr.EnableLlvm="false";
        PRAGMA Kikimr.KqpPushOlapProcess="true";
        PRAGMA TablePathPrefix("/foo/bar");
        PRAGMA AnsiInForEmptyOrNullableItemsCollections;
        DECLARE $log_level AS List<Utf8>;
        DECLARE $message0 AS Utf8;
        DECLARE $message1 AS Utf8;
        DECLARE $host AS List<Utf8>;
        DECLARE $pod AS List<Utf8>;
        DECLARE $box AS List<Utf8>;
        DECLARE $workload AS List<Utf8>;
        DECLARE $container_id AS List<Utf8>;
        DECLARE $logger_name AS List<Utf8>;
        DECLARE $continuation_seq AS Uint64;
        DECLARE $continuation_host AS Utf8;
        $message0_re = Hyperscan::Grep($message0);
        $message1_re = Hyperscan::Grep($message1);
        $timestamp_end = CAST("{ts_end_str}" AS Timestamp);
        $continuation_timestamp = CAST("{ts_continuation_str}" AS Timestamp);
        SELECT `timestamp`, `container_id`, `host`, `pod`, `box`, `workload`,
            `logger_name`, `user_id`, `request_id`, `message`,
            `log_level`, `seq`, `context`, `pod_transient_fqdn`, `pod_persistent_fqdn`,
            `node_fqdn`, `stack_trace`, `thread_name`, `log_level_int`
        FROM `{olap_table_name}`
        WHERE (`timestamp`, `host`, `seq`) >= ($continuation_timestamp, $continuation_host, $continuation_seq)
            AND `log_level` IN COMPACT $log_level
            AND ($message0_re(`message`) OR $message1_re(`message`))
            AND `host` NOT IN COMPACT $host
            AND `pod` IN COMPACT $pod
            AND `box` IN COMPACT $box
            AND `workload` IN COMPACT $workload
            AND `container_id` IN COMPACT $container_id
            AND `logger_name` IN COMPACT $logger_name
            AND `timestamp` <= $timestamp_end
        ORDER BY `timestamp` {order}, `host` {order}, `seq` {order}
        LIMIT 3
        """.format(
            ts_end_str=dt_to_str(end_dt),
            ts_continuation_str=dt_to_str(continuation_dt),
            olap_table_name=OlapTablesGenerator.TABLE_NAME,
            order=queries.ORDER_PB_TO_STR[order_pb],
        )
    )

    resp = ctl.search(
        table_path='/foo/bar',
        timestamp_range=(from_ts, to_ts),
        limit=3,
        order=order_pb,
        continuation_token=backward_token,
        **kwargs
    )

    assert_ts(resp, rows, ts_converter=ts_to_ydb_timestamp)

    calls = tc.method_calls
    assert len(calls) == 3
    n, args, kw = calls[2]
    assert n == 'scan_query'
    assert clean_query(args[0].yql_text) == expected_q
    assert args[1] == {
        u'$message0': ur'(?i)\Qqwerty1\E',
        u'$message1': ur'(?i)\Qqwerty2\E',
        u'$log_level': ['INFO', 'DEBUG'],
        u'$host': ['host1', 'host2'],
        u'$pod': ['pod1', 'pod2'],
        u'$box': ['box1', 'box2'],
        u'$workload': ['wl1', 'wl2'],
        u'$container_id': ['cnt1', 'cnt2'],
        u'$logger_name': ['log1', 'log2'],
        u'$continuation_host': rows[0]['host'],
        u'$continuation_seq': rows[0]['seq'],
    }

    continuation_dt = microseconds_to_dt(rows[0][queries.OlapQueryBuilder.TIMESTAMP_FLD])

    expected_q = clean_query(
        """
        PRAGMA Kikimr.EnableLlvm="false";
        PRAGMA Kikimr.KqpPushOlapProcess="true";
        PRAGMA TablePathPrefix("/foo/bar");
        PRAGMA AnsiInForEmptyOrNullableItemsCollections;
        DECLARE $log_level AS List<Utf8>;
        DECLARE $message0 AS Utf8;
        DECLARE $message1 AS Utf8;
        DECLARE $host AS List<Utf8>;
        DECLARE $pod AS List<Utf8>;
        DECLARE $box AS List<Utf8>;
        DECLARE $workload AS List<Utf8>;
        DECLARE $container_id AS List<Utf8>;
        DECLARE $logger_name AS List<Utf8>;
        DECLARE $continuation_seq AS Uint64;
        DECLARE $continuation_host AS Utf8;
        $message0_re = Hyperscan::Grep($message0);
        $message1_re = Hyperscan::Grep($message1);
        $timestamp_begin = CAST("{ts_begin_str}" AS Timestamp);
        $continuation_timestamp = CAST("{ts_continuation_str}" AS Timestamp);
        SELECT `timestamp`, `container_id`, `host`, `pod`, `box`, `workload`,
            `logger_name`, `user_id`, `request_id`, `message`,
            `log_level`, `seq`, `context`, `pod_transient_fqdn`, `pod_persistent_fqdn`,
            `node_fqdn`, `stack_trace`, `thread_name`, `log_level_int`
        FROM `{olap_table_name}`
        WHERE (`timestamp`, `host`, `seq`) <= ($continuation_timestamp, $continuation_host, $continuation_seq)
            AND `log_level` IN COMPACT $log_level
            AND ($message0_re(`message`) OR $message1_re(`message`))
            AND `host` NOT IN COMPACT $host
            AND `pod` IN COMPACT $pod
            AND `box` IN COMPACT $box
            AND `workload` IN COMPACT $workload
            AND `container_id` IN COMPACT $container_id
            AND `logger_name` IN COMPACT $logger_name
            AND $timestamp_begin <= `timestamp`
        ORDER BY `timestamp` {order}, `host` {order}, `seq` {order}
        LIMIT 3
        """.format(
            ts_begin_str=dt_to_str(begin_dt),
            ts_continuation_str=dt_to_str(continuation_dt),
            olap_table_name=OlapTablesGenerator.TABLE_NAME,
            order=queries.ORDER_PB_TO_STR[order_pb],
        )
    )

    resp = ctl.search(
        table_path='/foo/bar',
        timestamp_range=(from_ts, to_ts),
        limit=3,
        order=order_pb,
        continuation_token=current_token,
        **kwargs
    )

    assert_ts(resp, rows, ts_converter=ts_to_ydb_timestamp)

    calls = tc.method_calls
    assert len(calls) == 4
    n, args, kw = calls[3]
    assert n == 'scan_query'
    assert clean_query(args[0].yql_text) == expected_q
    assert args[1] == {
        u'$message0': ur'(?i)\Qqwerty1\E',
        u'$message1': ur'(?i)\Qqwerty2\E',
        u'$log_level': ['INFO', 'DEBUG'],
        u'$host': ['host1', 'host2'],
        u'$pod': ['pod1', 'pod2'],
        u'$box': ['box1', 'box2'],
        u'$workload': ['wl1', 'wl2'],
        u'$container_id': ['cnt1', 'cnt2'],
        u'$logger_name': ['log1', 'log2'],
        u'$continuation_host': rows[0]['host'],
        u'$continuation_seq': rows[0]['seq'],
    }


def test_context_key_suggests():
    rows = []
    rows.append(make_row(
        TS,
        context='{"pushclient_row_id": 41255}',
    ))
    rows.append(make_row(
        TS - 1,
        context='{"HOSTNAME": "sas1-ce5c66139f33.qloud-c.yandex.net", "pushclient_row_id": 39102, "version": 1}',
    ))
    rows.append(make_row(
        TS - 2,
        context='{"anyfield": "anyvalue", "anyobject": {"push_whatever": "you want"}}',
    ))
    rows.append(make_row(
        TS - 3,
        context='{"key.with.dots": "value.with.dots", "key": {"without": {"dots": "value_without_dots"}, "inner.with": {"dots": "such wow"}}}'
    ))

    tc = mock.MagicMock(spec=ydb.Session)
    sc = mock.MagicMock(spec=ydb.SchemeClient)
    sc.list_directory.return_value = make_list_directory(TS_DATE, 10)
    tc = mock.MagicMock(spec=ydb.TableClient)
    tc.scan_query.return_value = [mock.Mock(result_set=mock.Mock(rows=rows))]

    reg = metrics.Registry()
    ctl = controller.YdbLogsController(scheme_client=sc,
                                       table_client=tc,
                                       metrics_registry=reg,
                                       history_tables_count=100,
                                       pool_size=1,
                                       request_timeout=1)

    from_ts = timestamp_pb2.Timestamp()
    from_ts.FromMicroseconds(TS + 1)
    to_ts = timestamp_pb2.Timestamp()
    to_ts.FromMicroseconds(TS + 100)

    kwargs = dict(
        search_patterns=(['qwerty', 'qwerty2'], INCLUDE),
        log_levels=(['INFO', 'DEBUG'], INCLUDE),
        log_levels_int=None,
        hosts=(['host1', 'host2'], EXCLUDE),
        pods=(['pod1', 'pod2'], INCLUDE),
        boxes=(['box1', 'box2'], INCLUDE),
        workloads=(['wl1', 'wl2'], INCLUDE),
        containers=(['cnt1', 'cnt2'], INCLUDE),
        logger_names=(['log1', 'log2'], INCLUDE),
        pod_transient_fqdns=None,
        pod_persistent_fqdns=None,
        node_fqdns=None,
        thread_names=None,
        request_ids=None,
        stack_traces=None,
        user_fields=None,
        table_path='/foo/bar',
        continuation_token=None,
        timestamp_range=(from_ts, to_ts),
        limit=3,
        order=dproxy_pb2.DESC,
    )
    resp = ctl.get_context_keys(
        known_args=kwargs,
        key_prefix='',
    )
    assert set(resp) == {
        'pushclient_row_id',
        'HOSTNAME',
        'version',
        'anyfield',
        'anyobject',
        'anyobject.push_whatever',
        'key\\.with\\.dots',
        'key',
        'key.without',
        'key.without.dots',
        'key.inner\\.with',
        'key.inner\\.with.dots',
    }

    resp = ctl.get_context_keys(
        known_args=kwargs,
        key_prefix='pu',
    )
    assert set(resp) == {'pushclient_row_id'}

    resp = ctl.get_context_keys(
        known_args=kwargs,
        key_prefix='any',
    )
    assert set(resp) == {'anyfield', 'anyobject', 'anyobject.push_whatever'}

    resp = ctl.get_context_keys(
        known_args=kwargs,
        key_prefix='anyobject',
    )
    assert set(resp) == {'anyobject', 'anyobject.push_whatever'}

    resp = ctl.get_context_keys(
        known_args=kwargs,
        key_prefix='anyobject.pu',
    )
    assert set(resp) == {'anyobject.push_whatever'}

    resp = ctl.get_context_keys(
        known_args=kwargs,
        key_prefix='key'
    )
    assert set(resp) == {
        'key\\.with\\.dots',
        'key',
        'key.without',
        'key.without.dots',
        'key.inner\\.with',
        'key.inner\\.with.dots',
    }


def test_olap_context_key_suggests():
    rows = []
    rows.append(olap_make_row(
        TS,
        context='{"pushclient_row_id": 41255}',
    ))
    rows.append(olap_make_row(
        TS - 1,
        context='{"HOSTNAME": "sas1-ce5c66139f33.qloud-c.yandex.net", "pushclient_row_id": 39102, "version": 1}',
    ))
    rows.append(olap_make_row(
        TS - 2,
        context='{"anyfield": "anyvalue", "anyobject": {"push_whatever": "you want"}}',
    ))
    rows.append(make_row(
        TS - 3,
        context='{"key.with.dots": "value.with.dots", "key": {"without": {"dots": "value_without_dots"}, "inner.with": {"dots": "such wow"}}}'
    ))

    tc = mock.MagicMock(spec=ydb.Session)
    sc = mock.MagicMock(spec=ydb.SchemeClient)
    sc.list_directory.return_value = make_olap_list_directory()
    tc = mock.MagicMock(spec=ydb.TableClient)
    tc.scan_query.return_value = [mock.Mock(result_set=mock.Mock(rows=rows))]

    reg = metrics.Registry()
    ctl = controller.OlapYdbLogsController(scheme_client=sc,
                                           table_client=tc,
                                           metrics_registry=reg,
                                           history_tables_count=100,
                                           pool_size=1,
                                           request_timeout=1)

    from_ts = timestamp_pb2.Timestamp()
    from_ts.FromMicroseconds(TS + 1)
    to_ts = timestamp_pb2.Timestamp()
    to_ts.FromMicroseconds(TS + 100)

    kwargs = dict(
        search_patterns=(['qwerty', 'qwerty2'], INCLUDE),
        log_levels=(['INFO', 'DEBUG'], INCLUDE),
        log_levels_int=None,
        hosts=(['host1', 'host2'], EXCLUDE),
        pods=(['pod1', 'pod2'], INCLUDE),
        boxes=(['box1', 'box2'], INCLUDE),
        workloads=(['wl1', 'wl2'], INCLUDE),
        containers=(['cnt1', 'cnt2'], INCLUDE),
        logger_names=(['log1', 'log2'], INCLUDE),
        pod_transient_fqdns=None,
        pod_persistent_fqdns=None,
        node_fqdns=None,
        thread_names=None,
        request_ids=None,
        stack_traces=None,
        user_fields=None,
        table_path='/foo/bar',
        continuation_token=None,
        timestamp_range=(from_ts, to_ts),
        limit=3,
        order=dproxy_pb2.DESC,
    )
    resp = ctl.get_context_keys(
        known_args=kwargs,
        key_prefix='',
    )
    assert set(resp) == {
        'pushclient_row_id',
        'HOSTNAME',
        'version',
        'anyfield',
        'anyobject',
        'anyobject.push_whatever',
        'key\\.with\\.dots',
        'key',
        'key.without',
        'key.without.dots',
        'key.inner\\.with',
        'key.inner\\.with.dots',
    }

    resp = ctl.get_context_keys(
        known_args=kwargs,
        key_prefix='pu',
    )
    assert set(resp) == {'pushclient_row_id'}

    resp = ctl.get_context_keys(
        known_args=kwargs,
        key_prefix='any',
    )
    assert set(resp) == {'anyfield', 'anyobject', 'anyobject.push_whatever'}

    resp = ctl.get_context_keys(
        known_args=kwargs,
        key_prefix='anyobject',
    )
    assert set(resp) == {'anyobject', 'anyobject.push_whatever'}

    resp = ctl.get_context_keys(
        known_args=kwargs,
        key_prefix='anyobject.pu',
    )
    assert set(resp) == {'anyobject.push_whatever'}

    resp = ctl.get_context_keys(
        known_args=kwargs,
        key_prefix='key'
    )
    assert set(resp) == {
        'key\\.with\\.dots',
        'key',
        'key.without',
        'key.without.dots',
        'key.inner\\.with',
        'key.inner\\.with.dots',
    }
