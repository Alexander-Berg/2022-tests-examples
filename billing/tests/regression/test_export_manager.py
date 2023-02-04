import time
from datetime import datetime as dt
from textwrap import dedent
import pytest

from dwh.grocery.tools import (
    peek,
    create_engine,
)
from dwh.grocery.task.yt_export_task import (
    ExportManager,
    to_iso,
    to_oracle_time,
    DML,
)
from dwh.grocery.targets.db_targets.oracle_table_target import ComparableFieldChunkTarget

from . import insert_into_table

SOURCE_TABLE = "T_SHIPMENT"
TEST_TABLE = "T_TEST_EXPORT_MANAGER"
YB_CON_STR = "balance/balance"
SCHEMA = "bo"


class TestExportManager:

    @pytest.mark.skip(reason='cx_Oracle.DatabaseError: ORA-08186: invalid timestamp specified')
    def test_export_from_past(self, fake_t_shipment):
        update_id = to_iso(dt.now())
        # update_id_pre = to_iso(dt.now() - td(minutes=1))

        # ждём чуть больше минуты, чтобы тест можно было безбоязненно пускать несколько раз без перерыва
        time.sleep(65)

        # Формируем неважно какую запись но со временем модификации = времени начала теста
        sample_record, _ = peek(fake_t_shipment.read())
        n_r = {
            **sample_record,
            'dt': to_oracle_time(update_id),
            'update_dt': to_oracle_time(update_id),
        }

        # вставляем эту запись её
        insert_into_table(fake_t_shipment, [n_r])

        em = ExportManager(fake_t_shipment, YB_CON_STR)   # Менеджер по текущему состоянию базы
        em_flashback = ExportManager(fake_t_shipment, YB_CON_STR, update_id)  # Менеджер по состоянию минута до запуска теста

        # Найти сроки модификации по дмл > минута до начала теста.
        # Новая запись попадает, но во флэшбеке за то же время (минута до начала теста) её быть не должно
        params = (DML, update_id)

        new_rows = list(em.modified_rows(params))
        new_rows_flashback = list(em_flashback.modified_rows(params))
        assert len(new_rows) - len(new_rows_flashback) == 1

    @pytest.mark.skip(reason='cx_Oracle.DatabaseError: ORA-08186: invalid timestamp specified')
    def test_chunk_export_from_past(self, fake_t_shipment):
        update_id = to_iso(dt.now())
        time.sleep(65)

        sample_record, _ = peek(fake_t_shipment.read())

        p1_f = 148800
        p1_t = 148900

        n_r_p1 = {
            **sample_record,
            'dt': to_oracle_time(update_id),
            'update_dt': to_oracle_time(update_id),
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
            'dt': to_oracle_time(update_id),
            'update_dt': to_oracle_time(update_id),
            'bucks': 133710
        }

        chunk2 = ComparableFieldChunkTarget(
            (
                fake_t_shipment,
                ('bucks', p2_f, p2_t)
            )
        )

        insert_into_table(fake_t_shipment, [n_r_p2] * 15)

        em_ch_1 = ExportManager(chunk1, YB_CON_STR)  # Менеджер по текущему состоянию базы
        em_ch_2 = ExportManager(chunk2, YB_CON_STR)  # Менеджер по текущему состоянию базы

        em_ch_1_flashback = ExportManager(
            chunk1,
            YB_CON_STR,
            update_id,
        )

        em_ch_2_flashback = ExportManager(
            chunk2,
            YB_CON_STR,
            update_id,
        )

        params = (DML, update_id)

        m_rows_ch_1 = list(em_ch_1.modified_rows(params))
        m_rows_ch_2 = list(em_ch_2.modified_rows(params))
        flashback_m_rows_ch_1 = list(em_ch_1_flashback.modified_rows(params))
        flashback_m_rows_ch_2 = list(em_ch_2_flashback.modified_rows(params))

        assert len(m_rows_ch_1) - len(flashback_m_rows_ch_1) == 10
        assert len(m_rows_ch_2) - len(flashback_m_rows_ch_2) == 15

    def test_batch_delete(self, fake_t_shipment):
        con = create_engine(YB_CON_STR)

        def delete_n(table, n):
            r = con.execute(dedent(f"""
            DELETE FROM {table.intrabase_uri}
            WHERE rowid in (
                SELECT rowid
                FROM {table.intrabase_uri}
                FETCH FIRST {n} ROWS ONLY
            )
            """))
            assert r == n

        # r = con.execute(f"DELETE FROM {fake_t_shipment.intrabase_uri} WHERE 1 = 1")
        for _ in range(5):
            delete_n(fake_t_shipment, 5)
        assert len(list(fake_t_shipment.read())) == 25
        # assert r == 50
