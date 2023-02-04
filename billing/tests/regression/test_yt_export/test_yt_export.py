from datetime import (
    datetime as dt,
    timedelta as td
)
import time
from typing import (
    List,
    Dict,
)
from hashlib import sha256
import luigi
import pytest

from dwh.grocery.targets.yt_table_target import YTDynamicTableTarget
from dwh.grocery.targets.yt_table_target import generate_dynamic_table_columns_schema
from dwh.grocery.task.yt_export_task import (
    get_table_name_from_uri,
    get_dyntable_settings_from_bunker,
    YTStaticToDynamicTask,
)
from dwh.grocery.yt_export import (
    YTExport,
    MonoClusterYTExport,
    MultiClusterYTExport,
    preprocess_meta,
    safe_swap,
    YTExportToDynamic,
)
from dwh.grocery.task.yt_export_task import (
    YTExportTaskV2,
    YTFullExportTaskV2,
    YTExportDiffTaskV2,
    YTShagrenExportTaskV2,
    YTExportMonthlyTaskV2,
    YTExportSmallDataTask,
    YTAlwaysFullExportTask,
    ExportManager,
    Merger,
    ROWSCN,
    temp_path_v2)
from dwh.grocery.targets import (
    YTTableTarget,
    YTMapNodeTarget,
)
from dwh.grocery.targets.db_targets.oracle_table_target import (
    ComparableFieldChunkTarget,
    SpecialColumn,
    OracleTableTarget,
)
from dwh.grocery.tools import (
    CONF,
    nested_chain,
    peek,
    to_oracle_time,
    to_iso,
)

from .. import (
    insert_into_table,
    truncate_table,
    clean_export_changes,
    update_record,
    delete_record,
    as_set,
    is_eq
)
from ..strategies import rows_with_yt_schema


def before():
    return dt.now() - td(1)


def iso_now():
    return to_iso(dt.now())


def iso_before():
    return to_iso(before())


def freeze(d):
    return tuple(sorted(d.items()))


class TestYTExport:

    def test_preprocess_meta(self):
        data = {
            "name": "LoLKek",
            "source_uri": "meta/meta:bo.group_order_act_div_t",
            "chunks": {
                "chunk_field": "auto",
                "chunks_size": 15
            },
            "target": "",
            "full_data": "",
            "rename": "",
            "type": "export"
        }
        preprocesed = preprocess_meta(data)
        assert preprocesed == {
            "name": "LoLKek",
            "source_uri": "meta/meta:bo.group_order_act_div_t",
            "chunks": (SpecialColumn.auto, 15),
            "target": "",
            "full_data": "",
            "rename": "",
            "type": "export"
        }
        data = {
            **data,
            "chunks": {
                "chunk_field": "",
                "chunks_size": 15
            },
        }
        preprocesed = preprocess_meta(data)
        assert preprocesed == {
            "name": "LoLKek",
            "source_uri": "meta/meta:bo.group_order_act_div_t",
            "chunks": None,
            "target": "",
            "full_data": "",
            "rename": "",
            "type": "export"
        }

    def test_merger_io(self, yt_test_root):
        merger = Merger(
            cluster=yt_test_root.cluster,
            input_paths={
                'modified': "/ololo",
                'export': "/kekeke",
                'deleted': "/pysch",
            },
            output_path="/azaza",
            columns=['lol', 'kek', 'cheburek'],
            full_key=['lol', 'kek'],
            key='id',
        )
        code_inputs = set(merger.list_inputs())
        task_inputs = set(merger.format_targets(merger.input(), 'input').keys())
        assert (
            code_inputs
            == task_inputs
            == {'export_input', 'modified_input', 'deleted_input'}
        ), (code_inputs, task_inputs)
        code_outputs = set(merger.list_outputs())
        task_outputs = set(merger.format_targets(merger.output()).keys())
        assert (
            code_outputs
            == task_outputs
            == {'output'}
        ), (code_outputs, task_outputs)

    def test_merger_filter_by_deleted(self):
        schema = [{'name': 'id', 'type': 'int64'}, {'name': 'a', 'type': 'int64'}]

        export = YTTableTarget(CONF['YT']['TEMP'] + 'export_test', schema=schema)
        modified = YTTableTarget(CONF['YT']['TEMP'] + 'modified_test', schema=schema)
        deleted = YTTableTarget(CONF['YT']['TEMP'] + 'deleted_test', schema=schema[0:1:])
        output = YTTableTarget(CONF['YT']['TEMP'] + 'output_test', schema=schema)

        export.write([
            {'id': 0, 'a': 1},
            {'id': 1, 'a': 2},
            {'id': 2, 'a': 3},
        ])
        modified.write([
            {'id': 1, 'a': 22},
        ])
        deleted.write([
            {'id': 2},
        ])

        merger = Merger(
            input_paths={
                'modified': modified.path,
                'export': export.path,
                'deleted': deleted.path,
            },
            output_path=output.path,
            columns=['id', 'a'],
            full_key=['id', ],
            key='id',
        )
        merger.run()

        records = output.read()

        assert set(freeze(r) for r in records) == {
            freeze({'id': 0, 'a': 1}),
            freeze({'id': 1, 'a': 22}),
        }, records

        export.clear()
        modified.clear()
        deleted.clear()
        output.clear()

    def test_merger_filter_by_empty(self, yt_test_root):
        schema = [{'name': 'id', 'type': 'int64'}, {'name': 'a', 'type': 'int64'}]

        export = yt_test_root / YTTableTarget('export_test', schema=schema)
        modified = yt_test_root / YTTableTarget('modified_test', schema=schema)
        deleted = yt_test_root / YTTableTarget('deleted_test', schema=schema[0:1:])
        output = yt_test_root / YTTableTarget('output_test', schema=schema)

        export.write([
            {'id': 0, 'a': 1},
            {'id': 1, 'a': 2},
            {'id': 2, 'a': 3},
        ])
        modified.write([
            {'id': 1, 'a': 22},
        ])
        deleted.write([
        ])

        merger = Merger(
            cluster=yt_test_root.cluster,
            input_paths={
                'modified': modified.path,
                'export': export.path,
                'deleted': deleted.path,
            },
            output_path=output.path,
            columns=['id', 'a'],
            full_key=['id', ],
            key='id',
        )
        merger.run()

        records = output.read()

        assert set(freeze(r) for r in records) == {
            freeze({'id': 0, 'a': 1}),
            freeze({'id': 1, 'a': 22}),
            freeze({'id': 2, 'a': 3}),
        }, records

        export.clear()
        modified.clear()
        deleted.clear()
        output.clear()

    def test_simplify_outputs(self):
        lololo = {
            'a': [
                (YTMapNodeTarget("//ololo/kekeke/t_invoice/"), YTTableTarget("t_invoice"))
            ],
            'b': [
                (YTMapNodeTarget("//ololo/kekeke/hitler/"), YTTableTarget("0666-14-88")),
                (YTMapNodeTarget("//ololo/kekeke/hitler/"), YTTableTarget("0666-14-89")),
                (YTMapNodeTarget("//ololo/kekeke/hitler/"), YTTableTarget("0666-14-90")),
            ],
        }
        s_lololo = MonoClusterYTExport.simplify_output(lololo)
        assert s_lololo == {
            'a': [
                YTTableTarget("//ololo/kekeke/t_invoice")
            ],
            'b': [
                YTMapNodeTarget("//ololo/kekeke/hitler/") / YTTableTarget("0666-14-88"),
                YTMapNodeTarget("//ololo/kekeke/hitler/") / YTTableTarget("0666-14-89"),
                YTMapNodeTarget("//ololo/kekeke/hitler/") / YTTableTarget("0666-14-90"),
            ],
        }

    def test_full_export_table(self, fake_t_shipment, yt_test_root):
        output = yt_test_root / YTTableTarget('fake_t_shipment_full', update_id="1488")

        task = YTFullExportTaskV2(source=fake_t_shipment, target=output)

        is_success = luigi.build([task], local_scheduler=True)
        assert is_success
        assert output.exists()
        assert len(list(fake_t_shipment.read())) == len(list(output.read()))
        assert output.get_attr("sql_text") != "", f"sql_text attribute of YT table {output._path} is empty"

    @staticmethod
    def _assert_dynamic_table_contains_records(dynamic_table_target: YTDynamicTableTarget,
                                               records: List[Dict],
                                               sorted_columns: List[str]):
        for record in records:
            key = {col: record[col] for col in sorted_columns}
            assert list(dynamic_table_target.lookup_rows([key])) == [record]

    def test_yt_static_to_dynamic_task(self, yt_test_root):
        """Проверяем выгрузку статической таблицы в динамическую - запуск задачи YTStaticToDynamicTask"""

        # создаем статическую таблицу
        records = [
            {'key': 0, 'value': 'abc'},
            {'key': 1, 'value': 'def'},
        ]
        schema = [
            {'name': 'key', 'type': 'int64'},
            {'name': 'value', 'type': 'string'},
        ]
        static_table: YTTableTarget = yt_test_root / YTTableTarget(path='test_upsert_static_table', schema=schema)
        static_table.clear()
        static_table.write(records=records)
        assert static_table.exists()

        # в эту таблицу будем выгружать данные
        dynamic_table: YTDynamicTableTarget = yt_test_root / YTDynamicTableTarget(path='test_upsert_dynamic_table')
        dynamic_table.clear()  # удаляем для чистоты теста

        def run_export_and_check(records):
            task = YTStaticToDynamicTask(
                source_static_path=static_table.path,
                target_dynamic_path=dynamic_table.path,
                table_name='test_upsert',
            )
            is_success = luigi.build([task], local_scheduler=True)
            assert is_success

            self._assert_dynamic_table_contains_records(
                dynamic_table_target=dynamic_table,
                records=records,
                sorted_columns=['key'])

        # динамическая таблица не существует
        assert not dynamic_table.exists()
        # проверяем эскпорт в несуществующую динамическую таблицу
        run_export_and_check(records)

        # теперь динамическая таблица существует
        assert dynamic_table.exists()
        # проверяем эскпорт в существующую динамическую таблицу
        run_export_and_check(records)

    def test_export_to_dynamic(self, yt_test_root, fake_t_shipment):
        """Проверяем запуск верхнеуровневой YTUpsertTDynamicExport"""

        table_name = "fake_t_shipment"

        # запускаем задачу выгрузки (в нашем случае - одна таблица)
        task = YTExportToDynamic(
            cluster=yt_test_root.real_cluster(),
            tables=[table_name],
            targets={
                table_name: yt_test_root.path,
            },
            meta_dict={
                table_name: {
                    "type": "full",
                    "source_uri": fake_t_shipment.uri,
                    "chunks": {},
                },
            }
        )
        is_success = luigi.build([task], local_scheduler=True)
        assert is_success

        # данные в динамической таблице должны соответствовать статической
        target = task.output()[0]  # type: YTDynamicTableTarget
        self._assert_dynamic_table_contains_records(
            dynamic_table_target=target,
            records=fake_t_shipment.read(),
            sorted_columns=['service_id', 'service_order_id'])

        # атрибуты динамической таблицы должны соответствовать настройкам в бункере
        dyn_settings = get_dyntable_settings_from_bunker(env='test', table_name=table_name)
        assert target.get_attr('primary_medium') == dyn_settings['primary_medium']
        assert target.get_attr('tablet_cell_bundle') == dyn_settings['tablet_cell_bundle']

    def test_read_dynamic_table_settings_from_bunker(self):
        """Проверяем чтение из бункера настроек динамической таблицы"""

        data = get_dyntable_settings_from_bunker(env='test', table_name='bo/t_test_export_manager')
        assert data['sorted_key'] == ["service_id", "service_order_id"]
        assert data['primary_medium'] == 'default'
        assert data['tablet_cell_bundle'] == 'balance'

    def test_full_export_chunk(self, fake_t_shipment, yt_test_root):
        output = yt_test_root / YTTableTarget('fake_t_shipment_only_1488_bucks')
        sample_record, _ = peek(fake_t_shipment.read())

        p1_f = 148800
        p1_t = 148900

        n_r_p1 = {
            **sample_record,
            'dt': to_oracle_time(dt(2025, 1, 15)),
            'update_dt': to_oracle_time(dt(2025, 1, 15)),
            'bucks': 148810
        }

        chunk = ComparableFieldChunkTarget(
            (
                fake_t_shipment,
                ('bucks', p1_f, p1_t)
            )
        )

        insert_into_table(fake_t_shipment, [n_r_p1] * 10)

        assert chunk.exists()
        task = YTFullExportTaskV2(source=chunk, target=output)

        is_success = luigi.build([task], local_scheduler=True)
        assert is_success
        assert len(list(chunk.read())) == len(list(output.read()))

    def test_export_table_with_json_column(self, yt_test_root):
        source = OracleTableTarget(('t_thirdparty_service', 'bo', 'balance/balance'))
        output = yt_test_root / YTTableTarget('t_thirdparty_service')
        task = YTFullExportTaskV2(source=source, target=output)

        is_success = luigi.build([task], local_scheduler=True)

        # проверяем, что luigi-таска завершилась
        assert is_success
        assert output.exists()
        output_rows = output.read()
        assert len(list(source.read())) == len(output_rows)

        # проверяем, что в json-колонках хранятся dict (json), а не строки
        expected_json_columns = ["paysys_type_cc_config",
                                 "force_partner_map",
                                 "product_mapping_config"]
        for row in output_rows:
            for col in expected_json_columns:
                assert row[col] is None or isinstance(row[col], dict)

    # tests/test_yt_export.py::TestYTExport::test_full_export_tables_by_chunks FAILED    из-за паршивой схемы
    def test_full_export_tables_by_chunks(self, fake_t_shipment, yt_test_root):
        output = yt_test_root / YTTableTarget('fake_t_shipment_by_chunks')
        assert output.cluster == yt_test_root.cluster
        sample_record, _ = peek(fake_t_shipment.read())
        truncate_table(fake_t_shipment)

        RECORDS_NUM = 20_000

        records = [
            {
                **sample_record,
                'dt': to_oracle_time(dt(2025, 1, 15)),
                'update_dt': to_oracle_time(dt(2025, 1, 15)),
                'bucks': i,
            }
            for i in
            range(RECORDS_NUM)
        ]
        insert_into_table(fake_t_shipment, records)

        task = YTFullExportTaskV2(
            source=fake_t_shipment,
            chunks=('bucks', 2000),
            target=output
        )
        is_success = luigi.build([task], workers=6, local_scheduler=True)
        assert is_success
        assert len(list(fake_t_shipment.read())) == len(list(output.read()))

    def test_diff_export_table_rowscn_strategy(self, fake_t_product, yt_test_root):

        full_output = yt_test_root / YTTableTarget(
            "diff_export/fake_t_product_full",
            schema=fake_t_product.get_yt_schema()
        )
        assert full_output.cluster == yt_test_root.cluster
        # diff_output = yt_test_root / YTTableTarget("diff_export/fake_t_product_diff")

        # assert fake_t_product.hints
        sample_record, _ = peek(fake_t_product.read())

        # print('sample record dt', sample_record)
        n_r_p1 = {
            **sample_record,
            'dt': to_oracle_time(dt(2025, 1, 15)),
            'activ_dt': to_oracle_time(dt(2025, 1, 15)),
            'price': 148810.0,
        }
        # print('new record dt', n_r_p1)
        insert_into_table(
            fake_t_product,
            [{**n_r_p1, 'id': i} for i in range(1000, 1010)]
        )
        # assert r == 10
        clean_export_changes(fake_t_product)

        full_output.write(fake_t_product.read())

        n_r = {'price': 666.0}
        r = update_record(fake_t_product, {'id': 1001}, n_r)
        assert r == 1

        diff_task = YTExportDiffTaskV2(
            source=fake_t_product,
            full_table=full_output,
            strategy=ROWSCN,
            update_id="cheburek",
        )

        # print("DELETED", diff_task.get_deleted_table().path)
        # print("MODIFIED", diff_task.get_modified_table().path)

        is_success = luigi.build(
            [diff_task],
            local_scheduler=True,
        )
        assert is_success

        exported_records = as_set(full_output.read())
        all_records = as_set(fake_t_product.read())

        # assert as_set([n_r]).issubset(all_records)

        d = exported_records ^ all_records

        assert not d

        assert full_output.get_attr("sql_text") != "", f"sql_text attribute of YT table {full_output._path} is empty"

    def test_diff_export_chunk(self, fake_t_product, yt_test_root):
        p1_f = 148800
        p1_t = 148900
        chunk = ComparableFieldChunkTarget(
            (
                fake_t_product,
                ('price', p1_f, p1_t)
            )
        )

        full_output = yt_test_root / YTTableTarget(
            "diff_export/fake_t_product_only_1488_price",
            schema=chunk.get_yt_schema()
        )
        # output = yt_test_root / YTTableTarget('fake_t_product_only_1488_price')
        sample_record, _ = peek(fake_t_product.read())

        # print('sample record dt', sample_record)
        n_r_p1 = {
            **sample_record,
            'dt': to_oracle_time(dt(2025, 1, 15)),
            'activ_dt': to_oracle_time(dt(2025, 1, 15)),
            'price': 148810.0,
        }
        # print('new record dt', n_r_p1)
        r = insert_into_table(
            fake_t_product,
            [{**n_r_p1, 'id': i} for i in range(1000, 1010)]
        )
        # assert r == 10
        clean_export_changes(fake_t_product)

        full_output.write(chunk.read())

        n_r = {'price': 666.0}
        r = update_record(fake_t_product, {'id': 1001}, n_r)
        assert r == 1

        diff_task = YTExportDiffTaskV2(
            source=chunk,
            full_table=full_output,
            strategy=ROWSCN,
            update_id=to_iso(dt.now() - td(minutes=1)),
        )

        is_success = luigi.build(
            [diff_task],
            local_scheduler=True,
        )
        assert is_success

        exported_records = as_set(full_output.read())
        all_records = as_set(chunk.read())

        # assert as_set([n_r]).issubset(all_records)

        d = exported_records ^ all_records

        assert not d
        assert full_output.get_attr("sql_text") != "", f"sql_text attribute of YT table {full_output._path} is empty"

    @pytest.mark.skip(reason="seems too long")
    def test_yt_export_task_for_table(self, fake_t_product, yt_test_root):
        """
        Выгрузить первично
        Проверить что всё сходится
        Изменить 1 запись, добавить 1 запись, удалить 1 запись
        Выгрузить ещё раз
        Проверить что сходится
        """
        print(f"FAKE URI {fake_t_product.uri}")
        output = yt_test_root / YTMapNodeTarget(
            "yt_export/fake_t_product/",
            # schema=fake_t_product.get_yt_schema()
        )

        output_m = yt_test_root / YTMapNodeTarget(
            "yt_export/fake_t_product_m/",
            # schema=fake_t_product.get_yt_schema()
        )

        sample_record, _ = peek(fake_t_product.read())
        n_r = {
            **sample_record,
            'dt': to_oracle_time(dt(2025, 1, 15)),
            'activ_dt': to_oracle_time(dt(2025, 1, 15)),
            'price': 148810.0,
        }
        insert_into_table(
            fake_t_product,
            [{**n_r, 'id': i} for i in range(1000, 1010)]
        )

        i_task = YTExportTaskV2(
            cluster=yt_test_root.real_cluster(),
            source_uri=fake_t_product.uri,
            initial=True,
            target=output.path,
        )

        is_success = luigi.build([i_task], local_scheduler=True)
        r_table = i_task.output()[0]
        assert is_success
        assert is_eq(r_table, fake_t_product)

        insert_into_table(fake_t_product, [{**n_r, 'id': 1488}])
        update_record(fake_t_product, {'id': 1001}, {'price': 666.0})
        delete_record(fake_t_product, {'id': 1002})

        assert not any(r['id'] == 1002 for r in fake_t_product.read())

        d_task = YTExportTaskV2(
            cluster=yt_test_root.real_cluster(),
            source_uri=fake_t_product.uri,
            full_data=output.path,
            target=output_m.path,
        )

        is_success = luigi.build([d_task], local_scheduler=True)
        assert is_success
        r_table = d_task.output()[0]

        assert is_eq(r_table, fake_t_product)

    def test_yt_export_dt_yt_for_table_diff_by_chunks(self, fake_t_payment, yt_test_root):
        """
        Выгрузить первично
        Проверить что всё сходится
        Изменить 1 запись, добавить 1 запись, удалить 1 запись
        Проверить что chunk_by для diff > 0
        Выгрузить ещё раз (выгрузка с chunk_by)
        Проверить что сходится
        """
        print(f"FAKE URI {fake_t_payment.uri}")
        print("Preparing test data")
        output = yt_test_root / YTMapNodeTarget(
            "yt_export/fake_t_payment/",
            # schema=fake_t_product.get_yt_schema()
        )

        output_m = yt_test_root / YTMapNodeTarget(
            "yt_export/fake_t_payment_m/",
            # schema=fake_t_product.get_yt_schema()
        )

        sample_record, _ = peek(fake_t_payment.read())

        dates = dict()
        for k, v in sample_record.items():
            if k == 'update_dt_yt':
                dates[k] = None
            elif 'dt' in k and v and isinstance(v, str):
                dates[k] = to_oracle_time(dt(2025, 1, 15))

        n_r = {**sample_record}
        n_r.update(dates)
        values = [{**n_r, 'id': i} for i in range(1000, 1010)]
        insert_into_table(fake_t_payment, values)

        print("Initial export")

        i_task = YTExportTaskV2(
            cluster=yt_test_root.real_cluster(),
            source_uri=fake_t_payment.uri,
            initial=True,
            target=output.path,
        )

        is_success = luigi.build([i_task], local_scheduler=True)
        r_table = i_task.output()[0]
        assert is_success

        print(f"Task {i_task} completed successfully")
        print(f"Check tables {r_table} and {fake_t_payment}")
        assert is_eq(r_table, fake_t_payment)

        print("Updating test table")
        update_dt_yt = to_oracle_time(dt.now())
        for i in range(1000, 1010):
            update_record(fake_t_payment, {'id': i}, {'register_id': i, 'update_dt_yt': update_dt_yt})

        time.sleep(5)  # ждем коммита транзакции, чтобы при выгрузке получить дифф.

        update_id = to_iso(dt.now())
        print("Exporting diff")
        d_task = YTExportTaskV2(
            cluster=yt_test_root.real_cluster(),
            source_uri=fake_t_payment.uri,
            full_data=output.path,
            target=output_m.path,
            update_id=update_id,
        )

        is_success = luigi.build([d_task], local_scheduler=True)
        assert is_success
        print(f"Task {d_task} completed successfully")

        exportable = d_task.requires()['exportable'].exportable
        assert exportable.diff_chunks_count == int(exportable.hints.get('diff_chunks_count'))

        d_table = d_task.output()[0]

        print(f"Check tables {d_table} and {fake_t_payment}")
        assert is_eq(d_table, fake_t_payment)
        print("Checking attributes")
        assert d_table.stored_update_id == update_id  # заполняем тем что передали в задачу
        assert d_table.get_attr('last_rowscn') == 0  # в этой стратегии не заполняется

    def test_yt_export_dt_yt_for_table(self, fake_t_order, yt_test_root):
        """
        Выгрузить первично
        Проверить что таблицы одинаковы
        Изменить запись
        Проверить, что chunk_by == 0 (выгрузка диффа без чанков)
        Проверить что таблицы сходятся
        ?? Проверить что update_id у таблицы выставлен корректно
        """
        print(f"URI: {fake_t_order.uri}")
        print("Preparing data")
        output = yt_test_root / YTMapNodeTarget("yt_export/fake_t_order/")
        output_m = yt_test_root / YTMapNodeTarget("yt_export/fake_t_order_m/")

        sample_record, _ = peek(fake_t_order.read())

        dates = dict()
        for k, v in sample_record.items():
            if k == 'update_dt_yt':
                dates[k] = None
            elif 'dt' in k and v and isinstance(v, str):
                dates[k] = to_oracle_time(dt(2025, 1, 15))
            elif 'date' in k:
                dates[k] = None

        n_r = {**sample_record}
        n_r.update(dates)
        values = [{**n_r, 'id': i} for i in range(1100, 1110)]
        insert_into_table(fake_t_order, values)

        print("Initial export")
        i_task = YTExportTaskV2(
            cluster=yt_test_root.real_cluster(),
            source_uri=fake_t_order.uri,
            initial=True,
            target=output.path,
        )

        is_success = luigi.build([i_task], local_scheduler=True)
        r_table = i_task.output()[0]
        assert is_success

        print(f"Check tables {r_table} and {fake_t_order}")
        assert is_eq(r_table, fake_t_order)

        print("Updating test table")
        update_dt_yt = to_oracle_time(dt.now())
        for i in range(1000, 1010):
            update_record(fake_t_order, {'id': i}, {'store_id': i, 'update_dt_yt': update_dt_yt})

        time.sleep(5)  # ждем коммита транзакции, чтобы получить дифф следующей выгрузкой

        update_id = to_iso(dt.now())

        print("Exporting diff")
        d_task = YTExportTaskV2(
            cluster=yt_test_root.real_cluster(),
            source_uri=fake_t_order.uri,
            full_data=output.path,
            target=output_m.path,
            update_id=update_id,
        )

        is_success = luigi.build([d_task], local_scheduler=True)
        assert is_success

        d_table = d_task.output()[0]

        print(f"Check tables {d_table} and {fake_t_order}")
        assert is_eq(d_table, fake_t_order)
        print("Checking attributes")
        assert d_table.stored_update_id == update_id
        assert d_table.get_attr('last_rowscn') == 0  # не заполняем в этой стратегии

    def test_yt_export_task_for_chunk(self, fake_t_product, yt_test_root):
        p1_f = 148800
        p1_t = 148900
        chunk = ComparableFieldChunkTarget(
            (
                fake_t_product,
                ('price', p1_f, p1_t)
            )
        )

        output = yt_test_root / YTMapNodeTarget(
            "yt_export/fake_t_product_1488/",
            # schema=fake_t_product.get_yt_schema()
        )

        output_m = yt_test_root / YTMapNodeTarget(
            "yt_export/fake_t_product_1488_m/",
            # schema=fake_t_product.get_yt_schema()
        )

        sample_record, _ = peek(fake_t_product.read())
        n_r = {
            **sample_record,
            'dt': to_oracle_time(dt(2025, 1, 15)),
            'activ_dt': to_oracle_time(dt(2025, 1, 15)),
            'price': 148810.0,
        }
        # print('new record dt', n_r_p1)
        insert_into_table(
            fake_t_product,
            [{**n_r, 'id': i} for i in range(1000, 1010)]
        )

        i_task = YTExportTaskV2(
            cluster=yt_test_root.real_cluster(),
            source_uri=chunk.uri,
            initial=True,
            target=output.path,
        )

        is_success = luigi.build([i_task], local_scheduler=True)
        r_table = i_task.output()[0]
        assert is_success
        assert is_eq(r_table, chunk)

        insert_into_table(fake_t_product, [{**n_r, 'id': 1488}])
        update_record(fake_t_product, {'id': 1001}, {'price': 148815.0})
        update_record(fake_t_product, {'id': 1003}, {'price': 666.0})
        delete_record(fake_t_product, {'id': 1002})

        d_task = YTExportTaskV2(
            cluster=yt_test_root.real_cluster(),
            source_uri=chunk.uri,
            full_data=output.path,
            target=output_m.path,
        )

        is_success = luigi.build([d_task], local_scheduler=True)
        assert is_success

        r_table = d_task.output()[0]
        assert is_eq(r_table, chunk)

    @pytest.mark.long
    def test_shagren(self, yt_test_root):
        target = yt_test_root / YTMapNodeTarget("shagren/group_order_act_div/")
        task = YTShagrenExportTaskV2(
            cluster=yt_test_root.real_cluster(),
            source_uri="meta/meta:bo.group_order_act_div_t",
            full_data="//home/balance/prod/bo/group_order_act_div/",
            target=target.path
        )
        is_success = luigi.build([task], workers=8, local_scheduler=True)
        assert is_success

    @pytest.mark.skip(reason="need to fix ORA-01466")
    def test_monthly_export(self, yt_test_root):
        target = yt_test_root / YTMapNodeTarget("monthly/t_ar_deal_stats/")
        task = YTExportMonthlyTaskV2(
            cluster=yt_test_root.real_cluster(),
            source_uri="meta/meta:bo.t_ar_deal_stats",
            full_data="//home/balance-test/dev/bo/t_ar_deal_stats/",
            target=target.path
        )
        is_success = luigi.build([task], workers=2, local_scheduler=True)
        assert is_success

    def test_monthly_export_new_month(self, yt_test_root):
        target = yt_test_root / YTMapNodeTarget("monthly/new_month/t_ar_deal_stats/")
        fake = target / YTTableTarget("hitler_did_nothing_wrong")
        fake.create()
        d_stats = "t_ar_deal_stats"
        task = MonoClusterYTExport(
            cluster=yt_test_root.real_cluster(),
            tables=[d_stats],
            targets={
                d_stats: target.path,
            },
            meta_dict={
                d_stats: {
                    "type": "monthly",
                    "source_uri": "meta/meta:bo.t_ar_deal_stats",
                    "chunks": {},
                },
            }
        )
        subtasks = task.create_subtasks([d_stats])
        d_stats_task = subtasks[d_stats]
        assert len(d_stats_task.output()) == 2
        is_success = luigi.build([task], workers=2, local_scheduler=True)
        assert is_success

    def test_monthly_chunks_entrypoint(self, yt_test_root):
        monthly_chunks = yt_test_root / YTMapNodeTarget("yt_export/monthly_chunks/")
        d_stats = "t_ar_deal_stats"

        task = MonoClusterYTExport(
            cluster=yt_test_root.real_cluster(),
            update_id="kekeke",
            tables=[d_stats],
            targets={
                d_stats: monthly_chunks.path,
            },
            meta_dict={
                d_stats: {
                    "type": "monthly",
                    "source_uri": "meta/meta:bo.t_ar_deal_stats",
                    "chunks": {},
                },
            }
        )

        subtasks = task.create_subtasks([d_stats])
        d_stats_task = subtasks[d_stats]

        assert d_stats_task.initial is True
        d_stats_task_outputs = d_stats_task.output()
        assert len(d_stats_task_outputs) > 2

        (monthly_chunks / YTTableTarget("fake")).create()

        subtasks = task.create_subtasks([d_stats])
        d_stats_task = subtasks[d_stats]

        assert d_stats_task.initial is False
        d_stats_task_outputs = d_stats_task.output()
        assert len(d_stats_task_outputs) == 2

    def test_small_data(self, yt_test_root, fake_t_product):
        target = yt_test_root / YTMapNodeTarget("small/fake_t_product/")
        task = YTExportSmallDataTask(
            cluster=yt_test_root.real_cluster(),
            source_uri=fake_t_product.uri,
            target=target.path,
            update_id="ololo",
            initial=True,
        )
        is_success = luigi.build([task], local_scheduler=True)
        assert is_success
        assert is_eq(task.output()[0], fake_t_product)

        sample_record, _ = peek(fake_t_product.read())
        n_r = {
            **sample_record,
            'dt': to_oracle_time(dt(2025, 1, 15)),
            'activ_dt': to_oracle_time(dt(2025, 1, 15)),
            'price': 148810.0,
        }
        insert_into_table(fake_t_product, [n_r])

        task = task.clone(initial=False, update_id="kekeke")
        is_success = luigi.build([task], local_scheduler=True)
        assert is_success
        assert is_eq(task.output()[0], fake_t_product)

    def test_always_full(self, yt_test_root, fake_t_product):
        target = yt_test_root / YTMapNodeTarget("small/fake_t_product/")
        task = YTAlwaysFullExportTask(
            cluster=yt_test_root.real_cluster(),
            source_uri=fake_t_product.uri,
            target=target.path,
            update_id="ololo",
            initial=True,
        )
        is_success = luigi.build([task], local_scheduler=True)
        assert is_success
        assert is_eq(task.output()[0], fake_t_product)

        sample_record, _ = peek(fake_t_product.read())
        n_r = {
            **sample_record,
            'dt': to_oracle_time(dt(2025, 1, 15)),
            'activ_dt': to_oracle_time(dt(2025, 1, 15)),
            'price': 148810.0,
        }
        insert_into_table(fake_t_product, [n_r])

        task = task.clone(initial=False, update_id="kekeke")
        is_success = luigi.build([task], local_scheduler=True)
        assert is_success
        assert is_eq(task.output()[0], fake_t_product)

    def test_export_entrypoint_internals(self, yt_test_root, fake_t_shipment, fake_t_product):
        full_t_shipment = yt_test_root / YTMapNodeTarget("yt_export/t_shipment_internal/T_TEST_EXPORT_MANAGER/")
        full_t_product = yt_test_root / YTMapNodeTarget("yt_export/t_product_internal/")

        full_t_product.create()
        full_t_shipment.create()

        fts = "fake_t_shipment"
        ftp = "fake_t_product"

        task = MonoClusterYTExport(
            cluster=yt_test_root.real_cluster(),
            update_id="kekeke",
            tables=["fake_t_shipment", "fake_t_product"],
            targets={
                "fake_t_shipment": full_t_shipment.path,
                "fake_t_product": full_t_product.path,
            },
            meta_dict={
                "fake_t_shipment": {
                    "type": "export",
                    "source_uri": fake_t_shipment.uri,
                    "chunks": {},
                },
                "fake_t_product": {
                    "type": "small",
                    "source_uri": fake_t_product.uri,
                    "chunks": {},
                },
            }
        )

        subtasks = task.create_subtasks([fts, ftp])
        fake_t_product_task = subtasks[ftp]
        fake_t_shipment_task = subtasks[fts]

        assert fake_t_product_task.source_uri == fake_t_product.uri
        assert fake_t_product_task.initial is True
        assert fake_t_product_task.target == ""
        assert fake_t_product_task.full_data == full_t_product.path
        assert fake_t_product_task.chunks is None

        assert fake_t_shipment_task.source_uri == fake_t_shipment.uri
        assert fake_t_shipment_task.initial is True
        assert fake_t_shipment_task.target == ""
        assert fake_t_shipment_task.full_data == full_t_shipment.path
        assert fake_t_shipment_task.chunks is None
        shipment_o = task.output()['fake_t_shipment'][0]
        assert shipment_o.path == full_t_shipment.path[:-1:]

        (full_t_product / YTTableTarget("fake")).create()
        (yt_test_root / YTTableTarget("yt_export/t_shipment_internal/T_TEST_EXPORT_MANAGER")).create()

        task = MonoClusterYTExport(
            cluster=yt_test_root.real_cluster(),
            update_id="kekeke",
            tables=["fake_t_shipment", "fake_t_product"],
            targets={
                "fake_t_shipment": full_t_shipment.path,
                "fake_t_product": full_t_product.path,
            },
            meta_dict={
                "fake_t_shipment": {
                    "type": "export",
                    "source_uri": fake_t_shipment.uri,
                    "chunks": {},
                },
                "fake_t_product": {
                    "type": "small",
                    "source_uri": fake_t_product.uri,
                    "chunks": {},
                },
            }
        )

        subtasks = task.create_subtasks([fts, ftp])
        fake_t_product_task = subtasks[ftp]
        fake_t_shipment_task = subtasks[fts]

        assert fake_t_product_task.source_uri == fake_t_product.uri
        assert fake_t_product_task.initial is False
        assert fake_t_product_task.target == ""
        assert fake_t_product_task.full_data == full_t_product.path
        assert fake_t_product_task.chunks is None
        assert fake_t_product_task.update_id == "kekeke"

        assert fake_t_shipment_task.source_uri == fake_t_shipment.uri
        assert fake_t_shipment_task.initial is False
        assert fake_t_shipment_task.target == ""
        assert fake_t_shipment_task.full_data == full_t_shipment.path
        assert fake_t_shipment_task.chunks is None
        assert fake_t_shipment_task.update_id == "kekeke"

        assert all(v.update_id == "kekeke" for v in nested_chain(task.output()))

    def test_export_entrypoint_default_targets(self, yt_test_root, fake_t_shipment, fake_t_product):
        full_t_shipment = yt_test_root / YTMapNodeTarget("yt_export/t_shipment_internal/")
        full_t_product = yt_test_root / YTMapNodeTarget("yt_export/t_product_internal/")

        full_t_product.create()
        full_t_shipment.create()

        fts = "fake_t_shipment"
        ftp = "fake_t_product"

        task = MonoClusterYTExport(
            cluster=yt_test_root.real_cluster(),
            update_id="kekeke",
            tables=["fake_t_shipment", "fake_t_product"],
            targets={
                "fake_t_product": full_t_product.path,
            },
            meta_dict={
                "fake_t_shipment": {
                    "type": "export",
                    "source_uri": fake_t_shipment.uri,
                    'target': full_t_shipment.path,
                    "chunks": {},
                },
                "fake_t_product": {
                    "type": "small",
                    "source_uri": fake_t_product.uri,
                    "rename": "product_t",
                    "chunks": {},
                },
            }
        )

        subtasks = task.create_subtasks([fts, ftp])
        fake_t_product_task_output = subtasks[ftp].output()[0]
        fake_t_shipment_task_output = subtasks[fts].output()[0]
        assert fake_t_product_task_output.as_leaf().path == "product_t"
        assert fake_t_shipment_task_output.as_leaf().path == fake_t_shipment.table_name

    def test_export_entrypoint_with_rename(self, yt_test_root, fake_t_shipment, fake_t_product):
        full_t_shipment = yt_test_root / YTMapNodeTarget("yt_export/t_shipment_internal/")
        full_t_product = yt_test_root / YTMapNodeTarget("yt_export/t_product_internal/")

        full_t_product.create()
        full_t_shipment.create()

        fts = "fake_t_shipment"
        ftp = "fake_t_product"

        task = MonoClusterYTExport(
            cluster=yt_test_root.real_cluster(),
            update_id="kekeke",
            tables=["fake_t_shipment", "fake_t_product"],
            targets={
                "fake_t_shipment": full_t_shipment.path,
                "fake_t_product": full_t_product.path,
            },
            meta_dict={
                "fake_t_shipment": {
                    "type": "export",
                    "source_uri": fake_t_shipment.uri,
                    "chunks": {},
                },
                "fake_t_product": {
                    "type": "small",
                    "source_uri": fake_t_product.uri,
                    "rename": "product_t",
                    "chunks": {},
                },
            }
        )

        subtasks = task.create_subtasks([fts, ftp])
        fake_t_product_task_output = subtasks[ftp].output()[0]
        fake_t_shipment_task_output = subtasks[fts].output()[0]
        assert fake_t_product_task_output.as_leaf().path == "product_t"
        assert fake_t_shipment_task_output.as_leaf().path == fake_t_shipment.table_name

    @pytest.mark.skip(reason="not fixed yet")
    def test_export_entrypoint(self, yt_test_root, fake_t_shipment, fake_t_product):

        fts = "fake_t_shipment"
        ftp = "fake_t_product"

        full_t_shipment = yt_test_root / YTMapNodeTarget("yt_export/t_shipment/")
        full_t_product = yt_test_root / YTMapNodeTarget("yt_export/t_product/")

        # m_t_shipment = yt_test_root / YTMapNodeTarget("yt_export/t_shipment_m/")
        # m_t_product = yt_test_root / YTMapNodeTarget("yt_export/t_product_m/")

        task = MonoClusterYTExport(
            cluster=yt_test_root.real_cluster(),
            update_id=to_iso(dt.now()),
            tables=["fake_t_shipment", "fake_t_product"],
            targets={
                "fake_t_shipment": full_t_shipment.path,
                "fake_t_product": full_t_product.path,
            },
            meta_dict={
                "fake_t_shipment": {
                    "type": "export",
                    "source_uri": fake_t_shipment.uri,
                    "chunks": {},
                },
                "fake_t_product": {
                    "type": "small",
                    "source_uri": fake_t_product.uri,
                    "chunks": {},
                },
            }
        )
        old_update_id = task.update_id
        assert not task.exported_before(fts)
        assert not task.exported_before(ftp)

        is_success = luigi.build([task], local_scheduler=True, workers=2)
        assert is_success

        outputs = task.output()
        fake_t_shipment_out = outputs['fake_t_shipment'][0]
        fake_t_product_out = outputs['fake_t_product'][0]

        assert fake_t_product_out.stored_update_id == task.update_id
        assert fake_t_shipment_out.stored_update_id == task.update_id

        assert is_eq(fake_t_shipment, fake_t_shipment_out)
        assert is_eq(fake_t_product, fake_t_product_out)

        sample_record, _ = peek(fake_t_product.read())
        n_r = {
            **sample_record,
            'dt': to_oracle_time(dt(2025, 1, 15)),
            'activ_dt': to_oracle_time(dt(2025, 1, 15)),
            'price': 148810.0,
        }
        insert_into_table(fake_t_product, [n_r])

        sample_record, _ = peek(fake_t_shipment.read())
        records = [
            {
                **sample_record,
                'dt': to_oracle_time(dt(2025, 1, 15)),
                'update_dt': to_oracle_time(dt(2025, 1, 15)),
                'bucks': i,
                'service_id': 1488,
            }
            for i in
            range(10)
        ]
        insert_into_table(fake_t_shipment, records)

        time.sleep(120)

        task = MonoClusterYTExport(
            cluster=yt_test_root.real_cluster(),
            update_id=to_iso(dt.now()),
            tables=["fake_t_shipment", "fake_t_product"],
            targets={
                "fake_t_shipment": full_t_shipment.path,
                "fake_t_product": full_t_product.path,
            },
            meta_dict={
                "fake_t_shipment": {
                    "type": "export",
                    "source_uri": fake_t_shipment.uri,
                    "chunks": {},
                },
                "fake_t_product": {
                    "type": "small",
                    "source_uri": fake_t_product.uri,
                    "chunks": {},
                },
            }
        )
        new_update_id = task.update_id
        assert old_update_id != new_update_id

        assert task.exported_before(fts)
        assert task.exported_before(ftp)

        is_success = luigi.build([task], local_scheduler=True, workers=2)
        assert is_success

        outputs = task.output()
        fake_t_shipment_out = outputs['fake_t_shipment'][0]
        fake_t_product_out = outputs['fake_t_product'][0]

        assert fake_t_product_out.stored_update_id == task.update_id
        assert fake_t_shipment_out.stored_update_id == task.update_id

        assert is_eq(fake_t_product, fake_t_product_out)

        fake_t_shipment_records_db = as_set(fake_t_shipment.read(update_id=new_update_id))
        fake_t_shipment_records_yt = as_set(fake_t_shipment_out.read())
        da = fake_t_shipment_records_db - fake_t_shipment_records_yt
        db = fake_t_shipment_records_yt - fake_t_shipment_records_db
        print(da)
        print(db)
        assert len(da) == 0
        assert len(db) == 0

    @pytest.mark.skip(reason="not fixed yet")
    def test_export_entrypoint_with_loss(self, yt_test_root, fake_t_shipment, fake_t_product):

        fts = "fake_t_shipment"
        ftp = "fake_t_product"

        full_t_shipment = yt_test_root / YTMapNodeTarget("yt_export/t_shipment/")
        full_t_product = yt_test_root / YTMapNodeTarget("yt_export/t_product/")

        # m_t_shipment = yt_test_root / YTMapNodeTarget("yt_export/t_shipment_m/")
        # m_t_product = yt_test_root / YTMapNodeTarget("yt_export/t_product_m/")

        task = MonoClusterYTExport(
            cluster=yt_test_root.real_cluster(),
            update_id=to_iso(dt.now()),
            tables=["fake_t_shipment", "fake_t_product"],
            targets={
                "fake_t_shipment": full_t_shipment.path,
                "fake_t_product": full_t_product.path,
            },
            meta_dict={
                "fake_t_shipment": {
                    "type": "export",
                    "source_uri": fake_t_shipment.uri,
                    "chunks": {},
                },
                "fake_t_product": {
                    "type": "small",
                    "source_uri": fake_t_product.uri,
                    "chunks": {},
                },
            }
        )
        old_update_id = task.update_id
        assert not task.exported_before(fts)
        assert not task.exported_before(ftp)

        is_success = luigi.build([task], local_scheduler=True, workers=2)
        assert is_success

        outputs = task.output()
        fake_t_shipment_out = outputs['fake_t_shipment'][0]
        fake_t_product_out = outputs['fake_t_product'][0]

        assert fake_t_product_out.stored_update_id == task.update_id
        assert fake_t_shipment_out.stored_update_id == task.update_id

        assert is_eq(fake_t_shipment, fake_t_shipment_out)
        assert is_eq(fake_t_product, fake_t_product_out)

        sample_record, _ = peek(fake_t_product.read())
        n_r = {
            **sample_record,
            'dt': to_oracle_time(dt(2025, 1, 15)),
            'activ_dt': to_oracle_time(dt(2025, 1, 15)),
            'price': 148810.0,
        }
        insert_into_table(fake_t_product, [n_r])

        sample_record, _ = peek(fake_t_shipment.read())
        records = [
            {
                **sample_record,
                'dt': to_oracle_time(dt(2025, 1, 15)),
                'update_dt': to_oracle_time(dt(2025, 1, 15)),
                'bucks': i,
            }
            for i in
            range(10)
        ]
        insert_into_table(fake_t_shipment, records)

        time.sleep(120)

        task = MonoClusterYTExport(
            update_id=to_iso(dt.now()),
            tables=["fake_t_shipment", "fake_t_product"],
            targets={
                "fake_t_shipment": full_t_shipment.path,
                "fake_t_product": full_t_product.path,
            },
            meta_dict={
                "fake_t_shipment": {
                    "type": "export",
                    "source_uri": fake_t_shipment.uri,
                    "chunks": {}
                },
                "fake_t_product": {
                    "type": "small",
                    "source_uri": fake_t_product.uri,
                    "chunks": {},
                },
            }
        )
        new_update_id = task.update_id
        assert old_update_id != new_update_id

        assert task.exported_before(fts)
        assert task.exported_before(ftp)

        is_success = luigi.build([task], local_scheduler=True, workers=2)
        assert is_success

        outputs = task.output()
        fake_t_shipment_out = outputs['fake_t_shipment'][0]
        fake_t_product_out = outputs['fake_t_product'][0]

        assert fake_t_product_out.stored_update_id == task.update_id
        assert fake_t_shipment_out.stored_update_id == task.update_id

        assert is_eq(fake_t_product, fake_t_product_out)

        fake_t_shipment_records_db = as_set(fake_t_shipment.read(update_id=new_update_id))
        fake_t_shipment_records_yt = as_set(fake_t_shipment_out.read())
        da = fake_t_shipment_records_db - fake_t_shipment_records_yt
        db = fake_t_shipment_records_yt - fake_t_shipment_records_db
        print(da)
        print(db)
        assert len(da) == 0
        assert len(db) == 0

    def test_small_export_without_remains(self, yt_test_root, fake_t_product):
        target = yt_test_root / YTMapNodeTarget("small/to_the_end/")
        export_manager = ExportManager(fake_t_product, fake_t_product.yb_connection_string)

        is_modified = export_manager.check_modification()   # by fact of creation
        assert is_modified

        task = YTExportSmallDataTask(
            cluster=yt_test_root.real_cluster(),
            source_uri=fake_t_product.uri,
            target=target.path,
            update_id="ololo",
        )
        is_success = luigi.build([task], local_scheduler=True)

        assert is_success
        assert is_eq(task.output()[0], fake_t_product)

        export_manager = ExportManager(fake_t_product, fake_t_product.yb_connection_string)
        is_modified = export_manager.check_modification()
        assert task.output()[0].get_attr('dml_dt') == to_iso(export_manager.max_dml_dt)
        assert task.output()[0].get_attr('last_rowscn') == export_manager.last_rowscn

    @pytest.mark.parametrize(
        "update_id_from,update_id_to,content",
        [
            (iso_now(), iso_before(), rows_with_yt_schema().example()[0]),
            (iso_before(), iso_before(), rows_with_yt_schema().example()[0]),
            (iso_now(), iso_before(), [])
        ]
    )
    def test_swap_safety(self, yt_test_root, update_id_from, update_id_to, content):

        ex_rows = [{'a': 1}]

        result_from = yt_test_root / YTTableTarget("swap_safety/from", update_id=update_id_from)
        result_from.write(content)

        result_to = yt_test_root / YTTableTarget("swap_safety/to", update_id=update_id_to)
        result_to.write(ex_rows)

        safe_swap(result_from, result_to)
        result_rows = as_set(result_to.read())
        if update_id_from >= update_id_to and content:
            assert result_rows == as_set(content)
        else:
            assert result_rows == as_set(ex_rows)

    def test_export_entrypoint_task_selection(self, hahn_yt_test_root, fake_t_shipment, fake_t_product):
        full_t_shipment = hahn_yt_test_root / YTMapNodeTarget("yt_export/t_shipment/")
        full_t_product = hahn_yt_test_root / YTMapNodeTarget("yt_export/t_product/")

        task = YTExport(
            update_id=to_iso(dt.now()),
            tables=["fake_t_shipment", "fake_t_product"],
            targets={
                "fake_t_shipment": full_t_shipment.path,
                "fake_t_product": full_t_product.path,
            },
            meta_dict={
                "fake_t_shipment": {
                    "type": "export",
                    "source_uri": fake_t_shipment.uri,
                    "chunks": {},
                },
                "fake_t_product": {
                    "type": "small",
                    "source_uri": fake_t_product.uri,
                    "chunks": {},
                },
            }
        )
        req_task = task.requires()
        assert isinstance(req_task, MonoClusterYTExport)
        task = task.clone(transfer_to_clusters=['arnold'])
        req_task = task.requires()
        assert isinstance(req_task, MultiClusterYTExport)

    @pytest.mark.skip(reason="not fixed yet")
    def test_export_entrypoint_with_transfer(
            self,
            yt_test_root,
            # arnold_yt_test_root,
            # hahn_yt_test_root,
            fake_t_product,
            fake_t_shipment,
    ):
        """
        1) Выгрузить t_product и t_shipment
        2) Проверить синк
        3) Записать какой-нибудь мусор в t_product
        4) Выгрузить t_product и t_shipment ещё раз
        5) проверить синк

        --- Нижняя часть пока что не актуальна, т.к. в ней должно проверяться отсутствие избыточности в выгрузке

        6) Выгрузить t_product и t_shipment ещё раз
        7) Проверить синк
        """

        def pair_outputs(mono_output, multi_output):
            hahn_t_shipmnet = mono_output['fake_t_shipment'][0]
            hahn_t_product = mono_output['fake_t_product'][0]

            arnold_tables: List[YTTableTarget] = multi_output['arnold']
            arnold_t_shipment = next(table for table in arnold_tables if ("t_shipment" in table.path))
            arnold_t_product = next(table for table in arnold_tables if ("t_product" in table.path))
            return [
                (hahn_t_product, arnold_t_product),
                (hahn_t_shipmnet, arnold_t_shipment),
            ]

        def export_and_check():
            upd_id = to_iso(dt.now())
            full_t_shipment = yt_test_root / YTMapNodeTarget("yt_export/t_shipment/")
            full_t_product = yt_test_root / YTMapNodeTarget("yt_export/t_product/")

            task = YTExport(
                transfer_to_clusters=['arnold'],
                update_id=upd_id,
                tables=["fake_t_shipment", "fake_t_product"],
                targets={
                    "fake_t_shipment": full_t_shipment.path,
                    "fake_t_product": full_t_product.path,
                },
                meta_dict={
                    "fake_t_product": {
                        "type": "small",
                        "source_uri": fake_t_product.uri,
                        "chunks": {},
                    },
                    "fake_t_shipment": {
                        "type": "export",
                        "source_uri": fake_t_shipment.uri,
                        "chunks": {},
                    },
                }
            )
            multi_cluster = task.requires()

            mono_cluster_output = multi_cluster.input()
            # base_t_product: YTTableTarget = mono_cluster_output['fake_t_product'][0]

            multi_cluster_output = multi_cluster.output()
            # arnold_t_product, arnold_t_shipment = multi_cluster_output['arnold']

            [
                (base_t_product, arnold_t_product),
                (base_t_shipmnet, arnold_t_shipment),
            ] = pair_outputs(mono_cluster_output, multi_cluster_output)

            assert not base_t_product.exists()
            assert not arnold_t_product.exists()

            is_success = luigi.build([task], local_scheduler=True, workers=2)
            assert is_success
            assert base_t_product.exists()
            assert arnold_t_product.exists()
            assert is_eq(base_t_product, arnold_t_product)
            assert is_eq(base_t_shipmnet, arnold_t_shipment)

        export_and_check()

        sample_record, _ = peek(fake_t_product.read())
        n_r = {
            **sample_record,
            'dt': to_oracle_time(dt(2025, 1, 15)),
            'activ_dt': to_oracle_time(dt(2025, 1, 15)),
            'price': 148810.0,
        }
        insert_into_table(fake_t_product, [n_r])

        time.sleep(60)

        export_and_check()

    def tests_temp_path_v2_rename_with_constant(self, fake_t_product, capsys):
        """
            Проверяет работу с параметром rename в temp_path_v2_rename.
            rename - константа
        """
        sha_uri = sha256(fake_t_product.uri.encode()).hexdigest()
        simple_name = temp_path_v2(fake_t_product, rename="simple_name")

        with capsys.disabled():
            print("simple_name ", simple_name)

        assert simple_name == f"//home/balance-test/test/tmp/{sha_uri}/simple_name"

    def tests_temp_path_v2_rename_with_formats(self, fake_t_product):
        """
            Проверяет работу с параметром rename в temp_path_v2_rename.
            rename - формат даты типа  %Y-%m-%d (DWH-554)
        """
        sha_uri = sha256(fake_t_product.uri.encode()).hexdigest()
        moscow_now = dt.now()
        update_id = to_iso(moscow_now)
        test_data = [
            ("", "%Y-%m-%dT%H:%M", ""),
            ("prefix", "%m-%dXXX%H:%M", ""),
            ("", "T%H:%MQ", "suffix"),
            ("some_prefix", "%Y-%m-%dT%H:%M", "some_suffix")
        ]

        for prefix, format, suffix in test_data:
            res = temp_path_v2(fake_t_product, rename=f"{prefix}{format}{suffix}", update_id=update_id)
            expected_t_name = f"{prefix}{moscow_now.strftime(format)}{suffix}"
            assert res == f"//home/balance-test/test/tmp/{sha_uri}/{expected_t_name}"

    def test_export_entrypoint_respect_bunker_path(self):
        task = MonoClusterYTExport(
            bunker_path="sad_export"
        )
        expected_meta = {
            'hah': {
                'source_uri': 'meta/meta:schema.table',
                'chunks': None,
                'target': '',
                'full_data': '',
                'rename': '',
                'type': 'export'
            }
        }
        #  в бункере может поменяться содержимое узла, а эту ветку трогать не будут
        meta = task.get_meta()
        assert meta['hah'] == expected_meta['hah']


def test_get_table_name_from_uri():
    assert get_table_name_from_uri('balance:bo.t_test_dynamic') == 'bo/t_test_dynamic'
    assert get_table_name_from_uri('balance:bo.T_TEST_DYNAMIC') == 'bo/t_test_dynamic'


class Test_generate_dynamic_table_columns_schema:
    """Тестирование генерации схемы динамической таблицы
    на основе схемы исходной таблицы и списка сортированных ключей динамической таблицы"""

    def test_empty(self) -> None:
        assert generate_dynamic_table_columns_schema([], []) == []

    def test_sort_order_property(self) -> None:
        """Сортированная колонка должна иметь свойство sort_order.
        Несортированная колонка не должна иметь свойство sort_order
        """

        source = [
            {'name': 'col1'},
            {'name': 'col2', 'sort_order': 'ascending'},
        ]
        sorted_key = [
            'col1',
        ]
        expected = [
            {'name': 'col1', 'sort_order': 'ascending'},
            {'name': 'col2'},
        ]
        assert generate_dynamic_table_columns_schema(source, sorted_key) == expected

    def test_columns_order(self) -> None:
        """Сортированные колонки должны находиться перед несортированными
        Порядок сортированных колонок должен соответствовать порядку в sorted_key
        Порядок несортированных колонок должен соответствовать порядку в исходной схеме
        """
        source = [
            {'name': 'col1'},
            {'name': 'col2'},
            {'name': 'col3'},
            {'name': 'col4'},
        ]
        sorted_key = [
            'col4',
            'col1',
        ]
        expected = [
            {'name': 'col4', 'sort_order': 'ascending'},
            {'name': 'col1', 'sort_order': 'ascending'},
            {'name': 'col2'},
            {'name': 'col3'},
        ]
        assert generate_dynamic_table_columns_schema(source, sorted_key) == expected

    def test_other_property_not_changed(self) -> None:
        """Другие свойства (атрибуты) колонок переносятся без изменений"""
        source = [
            {'name': 'col1', 'type': 't1', 'attr': 'val1'},
            {'name': 'col2', 'type': 't2', 'attr': 'val2'},
        ]
        sorted_key = [
            'col1',
        ]
        expected = [
            {'name': 'col1', 'sort_order': 'ascending', 'type': 't1', 'attr': 'val1'},
            {'name': 'col2', 'type': 't2', 'attr': 'val2'},
        ]
        assert generate_dynamic_table_columns_schema(source, sorted_key) == expected

    def test_absent_sorted_column(self) -> None:
        """Бросается исключение, если в исходной схеме таблицы отсутствует ожидаемая сортированная колонки"""
        source = [
            {'name': 'col1'},
            {'name': 'col2'}
        ]
        sorted_key = [
            'col4',
        ]
        with pytest.raises(Exception):
            assert generate_dynamic_table_columns_schema(source, sorted_key)
