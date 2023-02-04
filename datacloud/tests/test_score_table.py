# -*- coding: utf-8 -*-
from hamcrest import assert_that, equal_to
from datacloud.score_api.storage.ydb.ydb_tables.score_tables.score_table import ScoreTable
from datacloud.dev_utils.ydb.lib.for_tests.base_table_test import BaseTableTest


class TestScoresTable(BaseTableTest):

    _table = 'test-ydb-score-table'
    _TableClass = ScoreTable

    def test_insert_and_get(self):
        self._create_table_if_not_exists()
        table = self._TableClass(self._ydb_manager, self._database, self._table)
        Record = self._TableClass.Record

        hashed_cid, score = 222, 0.5
        input_record = Record(hashed_cid, score)

        table.insert([input_record])

        result_record = table.get_one(Record(hashed_cid))
        assert_that(input_record, equal_to(result_record))

        score = 0.8
        input_record = Record(hashed_cid, score)
        table.insert([input_record])
        result_record = table.get_one(Record(hashed_cid))
        assert_that(input_record, equal_to(result_record))
        self._drop_table_if_exists()
