# -*- coding: utf-8 -*-
from hamcrest import assert_that, equal_to, is_
from datacloud.score_api.storage.stability.ydb.storage import YdbStabilityStorage
from datacloud.score_api.storage.for_tests.base_storage_test import BaseStorageTest


class TestStabilityStorage(BaseStorageTest):

    stability_path = 'testing/stability/stability'

    @classmethod
    def setup_class(cls):
        super(TestStabilityStorage, cls).setup_class()
        cls._storage = YdbStabilityStorage(
            cls._ydb_manager,
            cls._database,
            TestStabilityStorage.stability_path)

    def test_get_stability(self):
        expected = '[[1,2,3],[4,5,6]]'
        assert_that(self._storage.get_stability('2018-10-23', 'partner_a', 'score_a', 'daily'),
                    equal_to(expected))

    def test_get_unexisting_stabilit(self):
        assert_that(self._storage.get_stability('unknown-date', 'partner_a', 'score_a', 'daily'),
                    is_(None))
