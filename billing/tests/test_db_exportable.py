import mock
import pytest

from dwh.grocery.targets.db_targets.base import (
    DBExportable,
    BUNKER,
)
from dwh.grocery.targets.db_targets.view_target import DBViewTarget
from dwh.grocery.targets.db_targets.oracle_table_target import OracleTableTarget, SpecialColumn
from dwh.grocery import tools


class TestDBExportable:

    def test_bunker_searching_happy_path(self):
        hard_sql_record = (
            {
                # "_schema": f"{BUNKER}{DBViewTarget.bunker_schema}",
                "yb_con": "balance/balance",
                "text": "select 1;",
                "lazy": False,
                "dumb": False,
                "columns": []
            },
            f"{BUNKER}{DBViewTarget.bunker_schema}"
        )
        with mock.patch.object(
                tools,
                "get_from_bunker",
                mock.Mock(return_value=hard_sql_record)
        ) as get_from_bunker_:
            exp = DBExportable.exportable_from_uri("bunker:/dwh/test/export/hard_sql_1")
            get_from_bunker_.assert_called_once_with("/dwh/test/export/hard_sql_1")
            assert isinstance(exp, DBViewTarget)
            assert exp.sql == hard_sql_record[0]['text']


class TestsDBTableTarget:

    @pytest.mark.skip(reason='need to fix')  # Unknown exportable for uri meta/meta:bo.mv_compl_30_days
    def test_chunk_key_passing(self):
        tbl: OracleTableTarget = DBExportable.exportable_from_uri("meta/meta:bo.mv_compl_30_days")
        # ORA-12154: TNS:could not resolve the connect identifier specified  (Но если указывать правильное соединение)
        assert tbl.chunk_by(SpecialColumn.auto, 16)[0].field_name == "order_id"
