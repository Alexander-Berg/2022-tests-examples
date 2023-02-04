# -*- coding: utf-8 -*-
from hamcrest import assert_that, equal_to
from datacloud.score_api.storage.ydb.ydb_tables.config_tables.score_path_table import ScorePathTable
from datacloud.dev_utils.ydb.lib.for_tests.base_table_test import BaseTableTest


class TestScorePathTable(BaseTableTest):

    _table = 'test-config-score-path-table'
    _TableClass = ScorePathTable

    def test_insert_and_get(self):
        self._create_table_if_not_exists()
        table = self._TableClass(self._ydb_manager, self._database, self._table)
        Record = self._TableClass.Record
        internal_score_name, score_path = 'internal-score-name', 'score/path/1'
        input_record = Record(internal_score_name, score_path)
        table.insert([input_record])
        result_record = table.get_one(Record(internal_score_name))
        assert_that(input_record, equal_to(result_record))

        score_path = 'score/path/2'
        input_record = Record(internal_score_name, score_path)
        table.insert([input_record])
        result_record = table.get_one(Record(internal_score_name))
        assert_that(input_record, equal_to(result_record))

        self._drop_table_if_exists()
