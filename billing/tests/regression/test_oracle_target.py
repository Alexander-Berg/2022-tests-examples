from datetime import (
    datetime as dt,
)
import time
import random as rnd
import pytest

from itertools import islice

from dwh.grocery.tools import (
    peek,
    to_oracle_time,
)
from dwh.grocery.targets.db_targets.oracle_table_target import (
    OracleTableTarget,
    ComparableFieldChunkTarget,
    DateChunkTarget,
    BucketChunkTarget,
    OverwatchType,
    HintsStorage,
)
from dwh.grocery.targets import ColumnFlags
from dwh.grocery.targets.db_targets import DBExportable

from . import (
    insert_into_table,
    truncate_table,
)


class TestOracleTarget:

    def test_bunker_hints(self):
        local_hints = {
            # "dst": "",
            "partition_key": "",
            "modified": "",
            "columns": [
                {
                    "type": "date",
                    "name": "payment_term_dt"
                },
                {
                    "type": "float",
                    "name": "rur_sum_e"
                },
                {
                    "type": "float",
                    "name": "rur_sum_d"
                },
                {
                    "type": "float",
                    "name": "rur_sum_c"
                },
                {
                    "type": "float",
                    "name": "rur_sum_b"
                },
                {
                    "type": "float",
                    "name": "rur_sum_a"
                },
                {
                    "type": "float",
                    "name": "usd_sum_c"
                },
                {
                    "type": "float",
                    "name": "amount_nsp"
                },
                {
                    "type": "date",
                    "name": "month_dt"
                },
                {
                    "type": "float",
                    "name": "amount_nds"
                },
                {
                    "type": "float",
                    "name": "currency_rate"
                },
                {
                    "type": "float",
                    "name": "amount"
                },
                {
                    "type": "float",
                    "name": "paid_amount"
                },
                {
                    "type": "date",
                    "name": "dt"
                },
                {
                    "type": "float",
                    "name": "act_sum"
                },
                {
                    "type": "float",
                    "name": "usd_rate"
                }
            ],
            "full_key": []
        }
        table = OracleTableTarget(('t_act_internal', 'bo', 'meta/meta'))
        assert local_hints == table.hints, dict(table.hints)

    def test_bunker_hints_tables_of_same_name(self):
        table_bo = OracleTableTarget(('t_payment', 'bo', 'balance/balance'))
        table_bs = OracleTableTarget(('t_payment', 'bs', 'balance/balance'))

        columns_names_bo = [c['name'] for c in table_bo.hints['columns']]
        columns_names_bs = [c['name'] for c in table_bs.hints['columns']]

        assert table_bo.hints != table_bs.hints
        assert "commission" in columns_names_bs
        assert "commission" not in columns_names_bo

    @pytest.mark.skip(reason='Something went wrong, need to fix?')
    def test_local_hints(self):
        local_hints = {
            # "dst": "",
            "partition_key": "",
            "modified": "",
            "columns": [
                {
                    "type": "date",
                    "name": "payment_term_dt"
                },
                {
                    "type": "float",
                    "name": "rur_sum_e"
                },
                {
                    "type": "float",
                    "name": "rur_sum_d"
                },
                {
                    "type": "float",
                    "name": "rur_sum_c"
                },
                {
                    "type": "float",
                    "name": "rur_sum_b"
                },
                {
                    "type": "float",
                    "name": "rur_sum_a"
                },
                {
                    "type": "float",
                    "name": "usd_sum_c"
                },
                {
                    "type": "float",
                    "name": "amount_nsp"
                },
                {
                    "type": "date",
                    "name": "month_dt"
                },
                {
                    "type": "float",
                    "name": "amount_nds"
                },
                {
                    "type": "float",
                    "name": "currency_rate"
                },
                {
                    "type": "float",
                    "name": "amount"
                },
                {
                    "type": "float",
                    "name": "paid_amount"
                },
                {
                    "type": "date",
                    "name": "dt"
                },
                {
                    "type": "float",
                    "name": "act_sum"
                },
                {
                    "type": "float",
                    "name": "usd_rate"
                }
            ],
            "full_key": []
        }

        table = OracleTableTarget(('t_act_internal', 'bo', 'meta/meta'), hints=HintsStorage.local)
        assert local_hints == table.hints, table.hints

    def test_new_table(self):
        table = DBExportable.exportable_from_uri("meta/meta:bo.group_order_act_div_t")
        assert isinstance(table, OracleTableTarget)
        assert table.schema == "bo"
        assert table.table == "group_order_act_div_t"
        assert table.yb_connection_string == "meta/meta"
        assert table.hints
        assert table.keyfield_name

    # def test_new_chunk(self):
    #     chunk = OracleExportable.exportable_from_uri("")

    def test_fake_table_read(self, fake_t_shipment: OracleTableTarget):
        records = list(fake_t_shipment.read())
        assert len(records) == 50

    def test_table_read(self):
        SOURCE_TABLE = "T_SHIPMENT"
        YB_CON_STR = "balance/balance"
        SCHEMA = "bo"
        source_table = OracleTableTarget((SOURCE_TABLE, SCHEMA, YB_CON_STR))
        records = list(islice(source_table.read(), 50))
        assert len(records) == 50

    def test_large_chunk_read(self, fake_t_shipment):
        sample_record, _ = peek(fake_t_shipment.read())

        n_r_p1 = {
            **sample_record,
            'dt': to_oracle_time(dt(2025, 1, 15)),
            'update_dt': to_oracle_time(dt(2025, 1, 15)),
        }

        insert_into_table(fake_t_shipment, [n_r_p1] * 15_000)    # Cursor size 10_000

        records = list(fake_t_shipment.read())
        assert len(records) == 15_050

    def test_date_chunk_read(self, fake_t_shipment: OracleTableTarget):
        """
        Добавим в фэйковую таблицу 10 записей за 2025 год и 15 за 2030
        """

        sample_record, _ = peek(fake_t_shipment.read())

        p1_f_date = dt(2025, 1, 2)
        p1_t_date = dt(2026, 1, 2)

        n_r_p1 = {
            **sample_record,
            'dt': to_oracle_time(dt(2025, 1, 15)),
            'update_dt': to_oracle_time(dt(2025, 1, 15)),
        }

        chunk1 = DateChunkTarget(
            (
                fake_t_shipment,
                ('dt', p1_f_date, p1_t_date)
            )
        )

        insert_into_table(fake_t_shipment, [n_r_p1] * 10)

        p2_f_date = dt(2030, 1, 15)
        p2_t_date = dt(2031, 1, 15)

        n_r_p2 = {
            **sample_record,
            'dt': to_oracle_time(dt(2030, 1, 15)),
            'update_dt': to_oracle_time(dt(2030, 1, 15)),
        }

        chunk2 = DateChunkTarget(
            (
                fake_t_shipment,
                ('dt', p2_f_date, p2_t_date)
            )
        )

        insert_into_table(fake_t_shipment, [n_r_p2] * 15)

        all_records = list(fake_t_shipment.read())
        assert len(all_records) == (50 + 10 + 15)
        chunk1_records = list(chunk1.read())
        assert len(chunk1_records) == 10
        chunk2_records = list(chunk2.read())
        assert len(chunk2_records) == 15

    def test_cmp_field_chunk_read(self, fake_t_shipment: OracleTableTarget):
        """
        Проверяем нарезку по полю BUCKS, хоть это и почти не имеет смысла
        """
        sample_record, _ = peek(fake_t_shipment.read())

        p1_f = 148800
        p1_t = 148900

        n_r_p1 = {
            **sample_record,
            'dt': to_oracle_time(dt(2025, 1, 15)),
            'update_dt': to_oracle_time(dt(2025, 1, 15)),
            'bucks': 148810
        }

        chunk1 = ComparableFieldChunkTarget(
            (
                fake_t_shipment,
                ('bucks', p1_f, p1_t)
            )
        )

        insert_into_table(fake_t_shipment, [n_r_p1] * 10)

        p2_f = 133700
        p2_t = 133800

        n_r_p2 = {
            **sample_record,
            'dt': to_oracle_time(dt(2025, 1, 15)),
            'update_dt': to_oracle_time(dt(2025, 1, 15)),
            'bucks': 133710
        }

        chunk2 = ComparableFieldChunkTarget(
            (
                fake_t_shipment,
                ('bucks', p2_f, p2_t)
            )
        )

        insert_into_table(fake_t_shipment, [n_r_p2] * 15)

        all_records = list(fake_t_shipment.read())
        assert len(all_records) == (50 + 10 + 15)
        chunk1_records = list(chunk1.read())
        assert len(chunk1_records) == 10
        chunk2_records = list(chunk2.read())
        assert len(chunk2_records) == 15

    def test_bucket_chunk_read(self, fake_t_shipment: OracleTableTarget):
        """
        Нарежем таблицу кусками по баксам
        """
        sample_record, _ = peek(fake_t_shipment.read())

        fake_t_shipment.engine.execute(f"""
            TRUNCATE TABLE {fake_t_shipment.intrabase_uri}
        """)

        new_records = [
            {
                **sample_record,
                'dt': to_oracle_time(dt(2025, 1, 15)),
                'update_dt': to_oracle_time(dt(2025, 1, 15)),
                'bucks': rnd.randint(0, 10000),
            }
            for _
            in range(200)
        ]
        insert_into_table(fake_t_shipment, new_records)

        num_of_buckets = 5

        b_chunks = [
            BucketChunkTarget(
                (
                    fake_t_shipment,
                    ('bucks', num_of_buckets, i),
                )
            )
            for i
            in range(num_of_buckets)
        ]
        records_in_buckets = [len(list(b.read())) for b in b_chunks]
        assert len(list(fake_t_shipment.read())) == 200
        assert sum(records_in_buckets) == 200
        assert max(records_in_buckets) < 200

    @pytest.mark.skip(reason="not fixed yet")
    def test_table_read_at(self, fake_t_shipment: OracleTableTarget):
        now = dt.now()

        update_id = now

        # ждём чуть больше минуты, чтобы тест можно было безбоязненно пускать несколько раз без перерыва
        time.sleep(65)
        # update_id_pre = now - td(minutes=1)

        sample_record, _ = peek(fake_t_shipment.read())

        # Формируем неважно какую запись но со временем модификации = времени начала теста
        n_r = {
            **sample_record,
            'dt': to_oracle_time(update_id),
            'update_dt': to_oracle_time(update_id),
        }

        # вставляем эту запись её
        insert_into_table(fake_t_shipment, [n_r])

        # Новая запись попадает, но во флэшбеке за то же время (минута до начала теста) её быть не должно
        all_records = list(fake_t_shipment.read())
        flashback_all_records = list(fake_t_shipment.read(update_id))
        assert len(all_records) == 51
        assert len(flashback_all_records) == 50

    def test_chunk_by(self, fake_t_shipment: OracleTableTarget):
        sample_record, _ = peek(fake_t_shipment.read())
        truncate_table(fake_t_shipment)

        records_num = 20_000
        from_ = int(dt.strptime("2017-01-01", "%Y-%m-%d").timestamp())
        to = int(dt.strptime("2027-01-01", "%Y-%m-%d").timestamp())

        records = [
            {
                **sample_record,
                'dt': to_oracle_time(dt.fromtimestamp(rnd.randint(from_, to))),
                'update_dt': to_oracle_time(dt.fromtimestamp(rnd.randint(from_, to))),
                'bucks': 1001 + i
            }
            for i in
            range(records_num)
        ]

        insert_into_table(fake_t_shipment, records)

        def chunks_test(chunks, num_of_chunks):
            chunks_rows = [len(list(chunk.read())) for chunk in chunks]
            assert len(chunks_rows) == num_of_chunks
            assert sum(chunks_rows) == records_num
            assert min(chunks_rows) > 0

        # buckets
        chunks_test(fake_t_shipment.chunk_by('bucks', 50), 50)
        chunks_test(fake_t_shipment.chunk_by('bucks', 100), 100)
        chunks_test(fake_t_shipment.chunk_by('bucks', 1), 1)

        # cmpfield
        chunks_test(fake_t_shipment.chunk_by('bucks', 2000), records_num // 2000)

        # datetime
        # TODO

    def test_watch_trigger_availability(self, fake_t_shipment, fake_t_product):
        assert not fake_t_shipment.overwatch_type() == OverwatchType.rowscn
        assert fake_t_product.overwatch_type() == OverwatchType.rowscn

    def test_constraint_check(self, fake_t_shipment):
        real_t_shipmnet = OracleTableTarget("balance/balance:bo.t_shipment")
        assert not fake_t_shipment.check_constraint()
        assert real_t_shipmnet.check_constraint()

    def test_columns_visibility(self):
        table = OracleTableTarget("balance/balance:bo.t_payment_register_line")
        mdi = table.modified_col_index()
        r, _ = peek(table.engine.execute(table.get_read_sql()))
        assert r[mdi] is not None

    def test_dt_patch(self):
        chunk: ComparableFieldChunkTarget = DBExportable.exportable_from_whatever(
            "balance/balance:bo.t_payment@id[2102,15002102]:c"
        )
        assert isinstance(chunk, ComparableFieldChunkTarget)
        assert ColumnFlags.crazy_dt in chunk.table.column('payment_dt').flags

    def test_multiple_dst_patch(self):
        table = OracleTableTarget.exportable_from_uri("meta/meta:bo.v_contract_apex_full")

        assert ColumnFlags.crazy_dt in table.column('is_signed').flags
        assert ColumnFlags.crazy_dt in table.column('is_cancelled').flags

    def test_get_diff_chunks_count(self):
        local_hints = {
            "partition_key": "",
            "dst": "",
            "modified": "",
            "full_key": [],
            "diff_chunks_count": 2,
            "columns": [
                {"name": "amount", "type": "float"},
                {"name": "commission", "type": "float"}
            ]
        }

        table = OracleTableTarget(('t_payment_register', 'bo', 'balance/balance'), hints=local_hints)
        assert table.diff_chunks_count == int(local_hints.get('diff_chunks_count'))

    def test_diff_chunks_count_is_empty(self):
        local_hints = {
            "partition_key": "",
            "dst": "",
            "modified": "",
            "full_key": [],
            "columns": [
                {"name": "amount", "type": "float"},
                {"name": "commission", "type": "float"}
            ]
        }

        table = OracleTableTarget(('t_payment_register', 'bo', 'balance/balance'), hints=local_hints)
        assert table.diff_chunks_count == 0
