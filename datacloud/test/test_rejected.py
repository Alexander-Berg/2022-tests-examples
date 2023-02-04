# -*- coding: utf-8 -*-
from hamcrest import assert_that, equal_to, greater_than, greater_than_or_equal_to
from datacloud.dev_utils.logging.logger import get_basic_logger
from datacloud.score_api.storage.ydb.ydb_tables.rejected_tables.rejected_requests_table import RejectedRequestsTable
from datacloud.score_api.storage.rejected.ydb.storage import YdbRejectedRequestsStorage
from datacloud.score_api.storage.for_tests.base_storage_test import BaseStorageTest


logger = get_basic_logger(__name__)


class TestYdbRejectedRequestsStorage(BaseStorageTest):

    rejected_requests_path = 'testing/rejected/rejected_requests'

    @classmethod
    def setup_class(cls):
        super(TestYdbRejectedRequestsStorage, cls).setup_class()
        cls._storage = YdbRejectedRequestsStorage(
            cls._ydb_manager,
            cls._database,
            TestYdbRejectedRequestsStorage.rejected_requests_path,
        )

    def test_ydb_add_rejected_request(self):
        logger.info('Start test_ydb_rejected_requests_storage, insert and request')
        record = RejectedRequestsTable.Record

        test_partner = 'test_partner'
        test_score = 'test_score'
        request_id = 'test_request_id'
        request_timestamp = 12345

        self._storage.reject_request(test_partner, test_score, request_id, request_timestamp)
        expected = record(test_partner, test_score, request_id, request_timestamp)
        actual = self._storage.get_rejected_request(test_partner, test_score, request_id)

        reject_timestamp = actual.reject_timestamp
        # Check reject_timestamp was set
        assert_that(reject_timestamp, greater_than(0))
        expected.reject_timestamp = reject_timestamp
        assert_that(actual, equal_to(expected))

        request_timestamp = 22222

        self._storage.reject_request(test_partner, test_score, request_id, request_timestamp)
        expected = record(test_partner, test_score, request_id, request_timestamp)
        actual = self._storage.get_rejected_request(test_partner, test_score, request_id)
        # Check reject_timestamp was set
        assert_that(actual.reject_timestamp, greater_than(0))
        # new reject timestamp is greater or equal than previous
        assert_that(actual.reject_timestamp,
                    greater_than_or_equal_to(reject_timestamp))

        reject_timestamp = actual.reject_timestamp
        expected.reject_timestamp = reject_timestamp
        assert_that(actual, equal_to(expected))
