# -*- coding: utf-8 -*-
from hamcrest import assert_that, equal_to
from datacloud.score_api.storage.ydb.ydb_tables.stability_tables.stability_table import StabilityTable
from datacloud.dev_utils.ydb.lib.for_tests.base_table_test import BaseTableTest


class TestStabilityTable(BaseTableTest):

    _table = 'test-ydb-stability-table'
    _TableClass = StabilityTable

    def test_insert_and_get(self):
        self._create_table_if_not_exists()
        table = self._TableClass(self._ydb_manager, self._database, self._table)
        Record = self._TableClass.Record

        date = '2019-01-25'
        partner_id = 'test-partner-1'
        score_id = 'test-score-1'
        segment = 'test-segment-1'
        stability = '[[0, 0, 0], [1, 1, 1]]'
        input_record = Record(date, partner_id, score_id, segment, stability)

        table.insert([input_record])

        result_record = table.get_one(Record(date, partner_id, score_id, segment))
        assert_that(input_record, equal_to(result_record))

        stability = '[[1, 1, 1], [2, 2, 2]]'
        input_record = Record(date, partner_id, score_id, segment, stability)
        table.insert([input_record])
        result_record = table.get_one(Record(date, partner_id, score_id, segment))

        assert_that(input_record, equal_to(result_record))

        self._drop_table_if_exists()
