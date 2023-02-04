# -*- coding: utf-8 -*-
from textwrap import dedent
from hamcrest import assert_that, equal_to
from datacloud.score_api.storage.ydb.ydb_tables.geo_tables.geo_logs_table import GeoLogsTable
from datacloud.dev_utils.ydb.lib.for_tests.base_table_test import BaseTableTest


class TestGeoLogsTable(BaseTableTest):

    _table = 'testing-geo-logs-table'
    _TableClass = GeoLogsTable

    def test_insert_and_get(self):
        self._create_table_if_not_exists()
        table = self._TableClass(self._ydb_manager, self._database, self._table)
        Record = self._TableClass.Record

        hashed_cid = 1
        points = dedent("""\
            ucftd3q09x ucfv1nkr3n ucftkm4jr2 ucfv1q2334 ucf7838pp2 ucftefsp2s uc9mt7q2yy ucfsucy9zg
            ucfv0ywz7p ucftdbg5c0 ucfv1p0huv ucft3q6ksj ucftpfrv6n ucfte5svde ucfts37fjx ucfte5ctq4
            ucfv1nx8z0 ucfwp4k93b ucftedy1gz ucfv1nb9pz ucftefs6tx ucftpz8n13 ucftd6x2b2 ucftqhnxjm
            ucftd3efnr ucftkm4jm6 ucftkm4evx ucftkm4jrp ucfv1ngbhp ucftd3q0kk ucfv1nkww7 ucfv1nertp
            ucftefettr ucfv1ndsee ucftd3jhd5 ucfv0ynwg1 ucfv1q916d ucfv1n7ndp ucftd3zgv6 ucftsb12m3
            ucftt018ee ucfv1nm6yh ucfts3n4bp ucftpr957e ucfte5v6d9 ucfts3j175 ucftkm1y1n ucfv2cu8v4
            ucfv1ne9eq ucfufnsk3m ucftkrzf82 ucftprdknd ucft3xdk2k ucftd3m9ub ucftd3ppee ucftd3q01n
            ucftd3m1wj""")

        input_record = Record(hashed_cid, points)
        table.insert([input_record])
        result_record = table.get_one(Record(hashed_cid))
        assert_that(input_record, equal_to(result_record))

        self._drop_table_if_exists()
