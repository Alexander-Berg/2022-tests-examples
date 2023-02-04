import time
from datetime import (
    datetime as dt,
    timedelta as td,
)
from textwrap import dedent
import mock
import pytest

from urllib.parse import urlencode
from yql.client import (
    operation as yql_operation,
    query as yql_query,
)
from yql.client.parameter_value_builder import YqlParameterValueBuilder
import luigi
from luigi import LocalTarget
from luigi.date_interval import Month

from dwh.grocery.task.yql_task import (
    YQLTask,
    EscapeYQLName,
    MaybeYQLTablesRange,
    YTTableTarget,
    Boost,
    CLOUD_BOOST,
    NIRVANA_BOOST,
    INPUT,
    OUTPUT,
    YqlSqlOperationWithQueryRequest,
)
from dwh.grocery.tools import (
    to_iso, SECRET
)
from dwh.grocery.targets import (
    TargetParameter,
)


CODE = """
INSERT into {output} with truncate
SELECT p.PageID,
       p.Name as domain,
       case when pp.PartnerID in (
       {KINOPOISK_CLIENT_ID},
       {AUTORU_CLIENT_ID},
       {VIP_SERGEY_CLIENT_ID},
       {YA_VERTICAL_CLIENT_ID},
       {YA_MARKET_CLIENT_ID},
       ) then pp.PartnerID else {EXTERNAL_PAGE} end as partnerid,
       case when p.OptionsYandexPage then {INTERNAL_PAGE} when pp.PartnerID = {MAILRU_CLIENT_ID} then {MAILRU_PAGE} else {EXTERNAL_PAGE} end as intpage,
       case when p.OptionsMainSerp then 1 else 0 end as mainserp,
       case when p.OptionsMobile then
       case when p.OptionsApp then {MOBILE_APP} else {MOBILE_PLACE} end else {NOT_MOBILE} end as is_mobile,
       case when p.OptionsBusinessUnit then 1 else 0 end as business_unit
FROM {partner_input} as pp
RIGHT JOIN {page_input} as p
ON p.PageID = pp.PageID
"""


class ExternalTable(luigi.ExternalTask):

    table = TargetParameter()

    def output(self):
        return self.table


class TestYQLTask:

    def test_escape_yql_name(self):
        trans_table = EscapeYQLName()
        assert len(trans_table) == 36
        assert sorted(trans_table) == [
            48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 97, 98, 99, 100, 101, 102, 103,
            104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117,
            118, 119, 120, 121, 122
        ]
        assert (
            "5Q2-фы)_%!$ва_mvxBйцукфCp%#(@6b7-A6ЫКОДЛсынцRdV!&h0G*%vT44TцеS3-dIHX8"
            .translate(trans_table) == (
                "5_2___________mvx_______p____6b7__6__________d___h0___v_44____3_d___8"
            )
        )

    def test_simple_format_targets(self):

        table = YTTableTarget(path="ololo")

        assert YQLTask.format_targets(table) == {'output': table}
        assert YQLTask.format_targets(table, 'input') == {'input': table}

    def test_list_format_output(self):

        table_1, table_2 = YTTableTarget(path="ololo"), YTTableTarget(path="kekeke")

        assert YQLTask.format_targets([table_1, table_2]) == {
            '0_output': table_1,
            '1_output': table_2,
        }

    def test_dict_format_output(self):

        table_1, table_2 = YTTableTarget(path="ololo"), YTTableTarget(path="kekeke")

        assert YQLTask.format_targets({'ololo': table_1, 'kekeke': table_2}) == {
            'ololo_output': table_1,
            'kekeke_output': table_2,
        }

    def test_complex_format_output(self):

        table_1, table_2, table_3, table_4 = (
            YTTableTarget(path="ololo"),
            YTTableTarget(path="kekeke"),
            YTTableTarget(path="1488"),
            YTTableTarget(path="666"),
        )
        s = {
            'AAA': {'CCC': table_1, 'DDD': table_2},
            'BBB': [table_3, table_4],
        }
        assert YQLTask.format_targets(s) == {
            'AAA_CCC_output': table_1,
            'AAA_DDD_output': table_2,
            'BBB_0_output': table_3,
            'BBB_1_output': table_4,
        }

    def test_dwh_387_input(self):
        table_1, table_2, table_3 = (
            YTTableTarget(path="ololo"),
            YTTableTarget(path="kekeke"),
            YTTableTarget(path="1488"),
        )

        s = {
            'sales_daily': table_1,
            'group_order_act_div': table_2,
            'cost_by_page': table_3,
        }

        assert YQLTask.format_targets(s, 'input') == {
            'sales_daily_input': table_1,
            'group_order_act_div_input': table_2,
            'cost_by_page_input': table_3,
        }, YQLTask.format_targets(s, 'input')

    def test_mixed_input(self):
        i = {
            'table': YTTableTarget(path="ololo"),
            'file': LocalTarget(is_tmp=True)
        }

        assert YQLTask.format_targets(i) == {
            'table_output': i['table'],
        }

    def test_mixed_with_range_input(self):
        month = Month.parse("1488-03")
        dates = month.dates()
        start, end = dates[0], dates[-1]
        tables_range = [YTTableTarget(f"/kek/{d}") for d in dates]
        table = YTTableTarget('kekeke')
        s = {
            'r': tables_range,
            'oh': table
        }
        res = YQLTask.format_targets(s, INPUT)
        assert res == {
            'r_input': MaybeYQLTablesRange("/kek", start.strftime("%Y-%m-%d"), end.strftime("%Y-%m-%d")),
            'oh_input': table,
        }, res

    def test_mixed_with_range_output(self):
        month = Month.parse("1488-03")
        dates = month.dates()
        tables_range = [YTTableTarget(f"/kek/{d}") for d in dates]
        table = YTTableTarget('kekeke')
        s = {
            'r': tables_range,
            'oh': table
        }
        res = YQLTask.format_targets(s, OUTPUT)
        assert 'r_output' not in res and isinstance(res.get('r_0_output', None), YTTableTarget)

    def test_find_all_sections(self):
        code = """
        {ku_ka_re_ku}
        {_e_1488}
        {_s_ololo_keke}
        {_s_ozozo}
        {_s_kek_s}
        {_s_kek_e}
        {azaza}
        {ololo_e}
        """
        sections = YQLTask.find_all_code_sections(code)
        assert sections == {
            "ololo_keke",
            "ozozo",
            "kek"
        }

    def test_find_inputs(self):
        code_inputs = YQLTask.find_all_inputs(CODE)
        assert code_inputs == {'partner_input', 'page_input'}, code_inputs

    def test_find_outputs(self):
        code_outputs = YQLTask.find_all_outputs(CODE)
        assert code_outputs == {'output'}, code_outputs

    def test_max_modtime_updateid_priority(self, yt_test_root):
        updid_a = to_iso(dt.now() - td(2))
        table_a = yt_test_root / YTTableTarget("test_update_id/a", update_id=updid_a)
        table_a.touch()

        class SomeLoneYQL(YQLTask):

            def requires(self):
                return [
                    ExternalTable(table=table_a),
                ]

        lone_yql = SomeLoneYQL()
        assert lone_yql.get_max_sources_modtime() == updid_a

    def test_max_modtime_getter(self, yt_test_root):
        updid = dt.now()

        updid_a = to_iso(updid - td(2))
        updid_b = to_iso(updid - td(1))

        table_a = yt_test_root / YTTableTarget("test_modtime/a", update_id=updid_a)
        table_b = yt_test_root / YTTableTarget("test_modtime/b", update_id=updid_b)
        table_c = yt_test_root / YTTableTarget("test_modtime/c")

        table_a.touch()
        table_b.touch()
        table_c.create()

        modtime = table_c.modification_time

        class SomeLoneYQL(YQLTask):

            def requires(self):
                return [
                    ExternalTable(table=table_a),
                    ExternalTable(table=table_b),
                    ExternalTable(table=table_c),
                ]
        lone_yql = SomeLoneYQL()
        assert lone_yql.get_max_sources_modtime() == modtime

        modtime = to_iso(updid)
        table_c.update_id = modtime
        table_c.touch()

        assert lone_yql.get_max_sources_modtime() == modtime

    def test_max_modtime_empty(self, yt_test_root):
        class SomeLoneYQL(YQLTask):
            pass
        lone_yql = SomeLoneYQL()
        assert lone_yql.get_max_sources_modtime() is None

    def test_max_modtime_single_dynamic_table(self, hahn_yt_test_root):
        """
           get_max_sources_modtime игнорирует динтаблицы.
        """
        updid_a = to_iso(dt.now() - td(2))
        a_schema = [dict(name="x", type="string")]
        dyntable_a = hahn_yt_test_root / YTTableTarget("test_update_id/dyn_a", update_id=updid_a,
                                                       sys_attrs={"dynamic": True}, schema=a_schema)
        dyntable_a.touch()

        class SomeLoneYQL(YQLTask):

            def requires(self):
                return [
                    ExternalTable(table=dyntable_a),
                ]

        lone_yql = SomeLoneYQL()
        assert dyntable_a.is_dynamic, f"{str(dyntable_a)} must be dynamic"
        assert lone_yql.get_max_sources_modtime() is None, f"max modtime of {str(dyntable_a)} must be ignored"

    def test_max_modtime_various_tables(self, hahn_yt_test_root):
        """
           get_max_sources_modtime игнорирует динтаблицы
        """

        updid = dt.now()

        updid_a = to_iso(updid - td(2))
        updid_b = to_iso(updid - td(1))

        table_a = hahn_yt_test_root / YTTableTarget("test_modtime/nondyn_a", update_id=updid_a)
        table_b = hahn_yt_test_root / YTTableTarget("test_modtime/nondyn_b", update_id=updid_b)
        schema_c = [dict(name="x", type="string")]
        table_c = hahn_yt_test_root / YTTableTarget("test_modtime/dyn_c", schema=schema_c,
                                                    sys_attrs={"dynamic": True})

        table_a.touch()
        table_b.touch()
        table_c.create()

        class SomeLoneYQL(YQLTask):

            def requires(self):
                return [
                    ExternalTable(table=table_a),
                    ExternalTable(table=table_b),
                    ExternalTable(table=table_c),
                ]

        lone_yql = SomeLoneYQL()
        assert table_a.is_dynamic is False, f"{str(table_a)} is static"
        assert table_b.is_dynamic is False, f"{str(table_b)} is static"
        assert table_c.is_dynamic is True, f"{str(table_c)} is dynamic"
        assert lone_yql.get_max_sources_modtime() == updid_b, f"max modtime of {str(table_c)} must be ignored"

    def test_max_modtime_hierarchy(self, hahn_yt_test_root):

        all_tables = [daniel, cooler_daniel, coolness] = [
            hahn_yt_test_root / YTTableTarget("yql_h/daniel", update_id="1"),
            hahn_yt_test_root / YTTableTarget("yql_h/cooler_daniel", update_id="2"),
            hahn_yt_test_root / YTTableTarget("yql_h/coolness", update_id="4"),
        ]

        class Main(YQLTask):

            def requires(self):
                return [Daniel(), CoolerDaniel()]

        class Daniel(YQLTask):

            def output(self):
                return daniel

        class CoolerDaniel(YQLTask):

            def requires(self):
                return Coolness()

            def output(self):
                return cooler_daniel

        class Coolness(luigi.ExternalTask):

            def output(self):
                return coolness

        for table in all_tables:
            table.create()
            table.touch()

        assert Main().get_max_sources_modtime() == "4"

    def test_desperate(self, yt_test_root):

        class LolKekYQL(YQLTask):
            yql = dedent("""
            insert into {output} with truncate
            select 1
            """)

            def output(self):
                return yt_test_root / YTTableTarget("desperate/desperate", update_id=to_iso(dt.now()))

        task = LolKekYQL()
        assert not task.complete()
        task.run()
        assert not task.complete()

        desperate_task = LolKekYQL(desperate=True)
        assert not desperate_task.complete()
        desperate_task.run()
        assert desperate_task.complete()

    def test_run_stored(self, test_config, yt_test_root):

        s_time = to_iso(dt.now())

        class LolKekYQL(YQLTask):
            yql_name = "test_run_stored"
            tags = ['test123']
            yql = dedent("""
                insert into {output} with truncate
                SELECT 1 as kek
            """)
            can_read = [test_config['MONITORING']['ROBOT']]

            def output(self):
                return yt_test_root / YTTableTarget("test_stored/one")

        task = LolKekYQL()
        task.run()
        output = task.output()
        rows = output.read()
        assert rows == [{'kek': 1}]

        last_run = yql_operation.YqlListOperationsRequest(
            urlencode({
                'filters': f"queryTitle={task.yql_name},status='COMPLETED',queryTags='test123'",
                'page_size': 1,
                'sort': "-updatedAt"
            })
        )
        last_run.run()
        assert last_run.is_ok

        last_run_operation_json = last_run.json['result'][0]
        assert last_run_operation_json['updatedAt'] > s_time

        saved_query_id, _version = task.get_query_id()
        assert last_run_operation_json['queryId'] == saved_query_id

    def test_run_multirun_stored(self, yt_test_root):

        now = dt.now()
        n = 2
        stime = 60

        class LolKekYQL(YQLTask):
            yql_name = f"test_run_multirun_stored_{now:%Y-%m-%d-%H-%M}"
            yql = dedent("""
                use hahn;
                insert into {output} with truncate
                select {k} as kek
            """)

            def run(self):
                super().run()
                time.sleep(stime)

            def output(self):
                return yt_test_root / YTTableTarget(f"multirun/m_{self.kwargs['k']}")

        tasks = [LolKekYQL(kwargs={'k': i}) for i in range(n)]
        pre = time.time()
        is_success = luigi.build(
            tasks,
            workers=n,
        )
        assert is_success
        query_id, _version = tasks[0].get_query_id()
        assert query_id is not None
        assert (time.time() - pre) < (stime * n)

    def test_name_escape(self):
        class LolKekYQL(YQLTask):
            yql_name = "ololo/kekeke:test.run@stored1488"

        assert LolKekYQL().escaped_yql_name == "ololo_kekeke_test_run_stored1488"

    def test_pragmas_formatter(self):
        pragmas = {"a": "b"}
        assert YQLTask.format_pragmas(pragmas) == (
            'PRAGMA a = "b";'
        )
        pragmas = {'yt.keke': "1488", 'yt.ololo': 'lol'}
        assert YQLTask.format_pragmas(pragmas) == (
            'PRAGMA yt.keke = "1488";\n'
            'PRAGMA yt.ololo = "lol";'
        )

    def test_boost_pragmas(self):

        class CloudBoosted(YQLTask):
            with_boost = Boost.cloud
        assert CloudBoosted().get_boost_pragmas() == CLOUD_BOOST

        class NirvanaBoosted(YQLTask):
            with_boost = Boost.nirvana
        assert NirvanaBoosted().get_boost_pragmas() == NIRVANA_BOOST

        class ForcedCloudBoosted(YQLTask):
            with_boost = Boost.nirvana
            with_cloud_boost = True
        assert ForcedCloudBoosted().get_boost_pragmas() == CLOUD_BOOST

        class NoBoosted(YQLTask):
            pass
        assert NoBoosted().get_boost_pragmas() == {}

    @pytest.mark.skip(reason='Not relevant. We have already migrated to yql v1.')
    @mock.patch('luigi.notifications.send_error_email')
    def test_yql_v1_fail(self, send_error_email: mock.MagicMock):
        v0_yql = """
        $get_date_from_timestamp_sec = ($timestamp) -> {
            return DateTime::ToStringFormat(
                DateTime::ToTimeZone(
                    DateTime::FromSeconds($timestamp),
                    'Europe/Moscow'),
                '%Y-%m-%d'
            );
        };

        select $get_date_from_timestamp_sec(1546290000);
        """

        class FailYQL(YQLTask):
            yql = v0_yql

        class NamedFailYQL(YQLTask):
            yql_name = "FailYQL"
            yql = v0_yql

        task = FailYQL()
        task.run()
        n_task = NamedFailYQL()
        n_task.run()

        assert send_error_email.call_count == 2

    @mock.patch('luigi.notifications.send_error_email')
    def test_yql_v1_nice(self, send_error_email: mock.MagicMock):
        v1_yql = """
        select 1;
        """

        class YQL(YQLTask):
            yql = v1_yql

        class NamedYQL(YQLTask):
            yql_name = "NiceYQLv1"
            yql = v1_yql

        task = YQL()
        task.run()
        n_task = NamedYQL()
        n_task.run()

        send_error_email.assert_not_called()

    @mock.patch('luigi.notifications.send_error_email')
    def test_yql_with_params(self, send_error_email: mock.MagicMock, yt_test_root, test_config):
        class YQL(YQLTask):
            yql = dedent(
                """
                DECLARE $param1 AS String;
                DECLARE $param2 AS Int64;

                insert into {output} with truncate
                select $param1 as p1, $param2 as p2;
                """
            )

            parameters = {
                '$param1': YqlParameterValueBuilder.make_string("T5yZWGxzUePRZT0hdI1wBQvD4pUaqXu"),
                '$param2': YqlParameterValueBuilder.make_int64(609672),
            }

            can_read = [test_config['MONITORING']['ROBOT']]

            def output(self):
                return yt_test_root / YTTableTarget("test_stored/yql_with_params")

        class NamedYQL(YQL):
            yql_name = "yql_with_params"

        task = YQL()
        task.run()
        send_error_email.assert_not_called()
        output = task.output()
        rows = output.read()
        assert rows == [{
            'p1': "T5yZWGxzUePRZT0hdI1wBQvD4pUaqXu",
            'p2': 609672,
        }]

        n_task = NamedYQL()
        n_task.run()
        send_error_email.assert_not_called()
        output = task.output()
        rows = output.read()
        assert rows == [{
            'p1': "T5yZWGxzUePRZT0hdI1wBQvD4pUaqXu",
            'p2': 609672,
        }]

    @mock.patch('luigi.notifications.send_error_email')
    def test_yql_with_attachments(self, send_error_email: mock.MagicMock, yt_test_root, test_config):
        class YQL(YQLTask):
            yql = dedent(
                """
                PRAGMA library('attachment.sql');

                IMPORT `attachment` SYMBOLS
                    $symbol1
                ;

                insert into {output} with truncate
                select $symbol1 as s1;
                """
            )

            attachments = [
                {
                    "name": "attachment.sql",
                    "content": dedent("""
                    $symbol1 = 'exported string constant';

                    export $symbol1;
                    """),
                },
                {
                    "type": "URL",
                    "content": "http://example.com",
                },
            ]

            can_read = [test_config['MONITORING']['ROBOT']]

            def output(self):
                return yt_test_root / YTTableTarget("test_stored/yql_with_params")

        class NamedYQL(YQL):
            yql_name = "yql_with_attachments"

        task = YQL()
        task.run()
        send_error_email.assert_not_called()
        output = task.output()
        rows = output.read()
        assert rows == [{
            's1': "exported string constant",
        }]

        n_task = NamedYQL()
        n_task.run()
        send_error_email.assert_not_called()
        output = task.output()
        rows = output.read()
        assert rows == [{
            's1': "exported string constant",
        }]


class TestYqlSqlOperationWithQueryRequest:

    @pytest.fixture(autouse=True)
    def setup(self):
        from yql.config import config as yql_config
        yql_config.token = SECRET['YQL_TOKEN']

        now = dt.now()
        self.yql = f"select '{now:%Y-%m-%d-%H-%M}';"

    @pytest.mark.parametrize(
        'yql_name',
        [
            pytest.param(None, id='noname'),
            pytest.param('somename', id='somename'),
        ],
    )
    def test_run(self, yql_name):

        operation_query = YqlSqlOperationWithQueryRequest(
            self.yql,
            title=yql_name,
        )

        operation_query.run()
        assert operation_query.is_ok

        operation_query.get_results(wait=True)
        assert operation_query.is_success

        operation_id = operation_query.operation_id
        operation_status = yql_operation.YqlOperationStatusRequest(operation_id)

        operation_status.run()
        assert operation_status.is_ok

        operation_status.get_results()
        assert operation_status.is_success

        assert operation_status.json['queryData']['content'] == self.yql

    def test_query_saved(self, test_config):
        query_title = 'some_query_name'
        query_tags = ['some_query_tag']
        can_read = [test_config['MONITORING']['ROBOT']]

        operation_query = YqlSqlOperationWithQueryRequest(
            self.yql,
            title=query_title,
            tags=query_tags,
        )

        operation_query.run(
            share_with=can_read
        )

        assert operation_query.is_ok

        operation_query.get_results(wait=True)
        assert operation_query.is_success

        query_id = operation_query.json['queryId']
        query = yql_query.YqlQueryRequestBase(query_id)

        query.run()
        assert query.is_ok

        assert query.json['title'] == query_title
        assert query.json['tags'] == query_tags
        assert query.json['acl']['canRead'] == can_read
        assert query.json['data']['content'] == self.yql

    @mock.patch('dwh.grocery.task.yql_task.yql_operation.YqlSqlOperationRequest.run')
    def test_is_ok_false(self, run_operation_mock):
        operation_query = YqlSqlOperationWithQueryRequest(
            self.yql,
            title='title'
        )

        with mock.patch('dwh.grocery.task.yql_task.yql_query.YqlQuerySaveRequest') as yql_query_mock:
            yql_query_mock.return_value.is_ok = False
            operation_query.run()

        assert not operation_query.is_ok
        run_operation_mock.assert_not_called()

    @mock.patch('dwh.grocery.task.yql_task.yql_operation.YqlSqlOperationRequest.run')
    def test_query_not_created_for_empty_title(self, run_operation_mock):
        operation_query = YqlSqlOperationWithQueryRequest(
            self.yql,
        )
        with mock.patch('dwh.grocery.task.yql_task.yql_query.YqlQuerySaveRequest') as yql_query_mock:
            operation_query.run()
            yql_query_mock.assert_not_called()

        assert run_operation_mock.call_count == 1
