# -*- coding: utf-8 -*-
from hamcrest import assert_that, equal_to
from datacloud.dev_utils.logging.logger import get_basic_logger
from datacloud.score_api.storage.scores.ydb.storage import YdbScoresMetaStorage
from datacloud.score_api.storage.for_tests.base_storage_test import BaseStorageTest

logger = get_basic_logger(__name__)


class TestScoreStorage(BaseStorageTest):

    partner_score_path = 'scores/partner_a/score_a/date-23-10-2018/'

    @classmethod
    def setup_class(cls):
        super(TestScoreStorage, cls).setup_class()
        cls._storage = YdbScoresMetaStorage(
            cls._ydb_manager,
            cls._database,
            'testing',
            TestScoreStorage.partner_score_path)

    def test_score_values(self):
        logger.info('Test get single score value')
        assert_that(self._storage._get_single_score_value(12345).score,
                    equal_to(0.8))

    def test_get_multiple_score_values(self):
        logger.info('Test get multiple score values')
        assert_that(self._storage._get_score_for_multiple_cids([12345, 67891]),
                    equal_to([0.8, 0.2341]))

    def test_get_scores_for_hashed_id_values(self):
        logger.info('Test get scores for hashed id values')
        assert_that(self._storage._get_score_for_multiple_cids([12345, 67891]),
                    equal_to([0.8, 0.2341]))

    # TODO: Add test for convert_id_values_to_hashed_id_values

    # TODO: Add test for get_scores_for_id_values
