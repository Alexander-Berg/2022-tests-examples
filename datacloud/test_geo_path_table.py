# -*- coding: utf-8 -*-
from hamcrest import assert_that, equal_to
from datacloud.score_api.storage.ydb.ydb_tables.config_tables.geo_path_table import GeoPathTable
from datacloud.dev_utils.ydb.lib.for_tests.base_table_test import BaseTableTest


class TestGeoPathTable(BaseTableTest):

    _table = 'test-config-geo-path-table'
    _TableClass = GeoPathTable

    def test_insert_and_get(self):
        self._create_table_if_not_exists()
        table = self._TableClass(self._ydb_manager, self._database, self._table)
        Record = self._TableClass.Record

        internal_score_name = 'test-internal-score-name'
        crypta_path, geo_path = 'test-crypta-table', 'test-geo-logs-table'
        weights = '1. 1. 1. 1. 1. 0. 0. 0. 0. 0. -1. -1. -1. -1. -1. 2. 3. 4.'

        input_record = Record(internal_score_name, crypta_path, geo_path, weights)
        table.insert([input_record])
        result_record = table.get_one(Record(internal_score_name))
        assert_that(input_record, equal_to(result_record))

        self._drop_table_if_exists()
