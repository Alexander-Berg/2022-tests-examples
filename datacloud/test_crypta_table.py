# -*- coding: utf-8 -*-
from hamcrest import assert_that, equal_to
from datacloud.score_api.storage.ydb.ydb_tables.score_tables.crypta_table import CryptaTable
from datacloud.dev_utils.ydb.lib.for_tests.base_table_test import BaseTableTest


class TestCryptaTable(BaseTableTest):

    _table = 'test-ydb-crypta-table'
    _TableClass = CryptaTable

    def test_insert_and_get(self):
        self._create_table_if_not_exists()

        table = self._TableClass(self._ydb_manager, self._database, self._table)
        Record = self._TableClass.Record

        hashed_id, hashed_cid = 111, 222
        input_record = Record(hashed_id, hashed_cid)
        table.insert([input_record])
        result_record = table.get_one(Record(hashed_id))
        assert_that(input_record, equal_to(result_record))

        hashed_cid = 333
        input_record = Record(hashed_id, hashed_cid)
        table.insert([input_record])
        result_record = table.get_one(Record(hashed_id))
        assert_that(input_record, equal_to(result_record))

        self._drop_table_if_exists()
