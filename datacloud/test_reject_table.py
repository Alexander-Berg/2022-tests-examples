from hamcrest import assert_that, equal_to
from datacloud.score_api.storage.ydb.ydb_tables.rejected_tables.rejected_requests_table import RejectedRequestsTable
from datacloud.dev_utils.ydb.lib.for_tests.base_table_test import BaseTableTest


class TestRejectTable(BaseTableTest):

    _table = 'testing-reject-table'
    _TableClass = RejectedRequestsTable

    def test_insert_and_get(self):
        self._create_table_if_not_exists()
        table = self._TableClass(self._ydb_manager, self._database, self._table)
        record = self._TableClass.Record

        partner_id = 'test_partner'
        score_id = 'test_score'
        request_id = 'test_request_id'
        request_timestamp = 12345
        reject_timestamp = 54321

        input_record = record(partner_id, score_id, request_id, request_timestamp, reject_timestamp)
        table.insert([input_record, ])
        result_record = table.get_one(record(partner_id, score_id, request_id))
        assert_that(input_record, equal_to(result_record))

        request_timestamp = 22222
        input_record = record(partner_id, score_id, request_id, request_timestamp, reject_timestamp)
        table.insert([input_record, ])
        result_record = table.get_one(record(partner_id, score_id, request_id))
        assert_that(input_record, equal_to(result_record))

        self._drop_table_if_exists()
