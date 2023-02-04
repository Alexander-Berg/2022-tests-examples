# -*- coding: utf-8 -*-
from hamcrest import assert_that, equal_to, is_
from datacloud.dev_utils.logging.logger import get_basic_logger
from datacloud.score_api.storage.score_path.ydb.storage import YdbScorePathStorage
from datacloud.score_api.storage.ydb.ydb_tables.config_tables.partner_scores_table import PartnerScoresTable
from datacloud.score_api.storage.for_tests.base_storage_test import BaseStorageTest

logger = get_basic_logger(__name__)


class TestYdbScorePathStorage(BaseStorageTest):

    @classmethod
    def setup_class(cls):
        super(TestYdbScorePathStorage, cls).setup_class()
        cls._storage = YdbScorePathStorage(
            cls._ydb_manager,
            cls._database,
            cls._path_config.partner_scores_table_path,
            cls._path_config.score_path_table_path)

    def test_ydb_score_path_storage_request(self):
        logger.info('Start test_ydb_score_path_storage, request')
        assert_that(self._storage.get_score_path('partner_a', 'score_a'),
                    equal_to('partner_a/score_a/date-23-10-2018'))

    def test_ydb_score_path_storage_unexisting_partner(self):
        logger.info('Start test_ydb_score_path_storage, unexisting partner')
        assert_that(self._storage.get_score_path('partner_NONE_PARTNER', 'score_a'),
                    is_(None))

    def test_ydb_score_path_storage_unexisting_score(self):
        logger.info('Start test_ydb_score_path_storage, unexisting score')
        assert_that(self._storage.get_score_path('partner_a', 'unexisting_score'),
                    is_(None))

    def test_get_user_scores(self):
        logger.info('Start test_ydb_score_path_storage, get user scores')
        expected = [
            PartnerScoresTable.Record('partner_a', 'score_a', 'partner_a-score_a', True),
            PartnerScoresTable.Record('partner_a', 'score_b', 'partner_a-score_b', True),
            PartnerScoresTable.Record('partner_a', 'score_c', 'partner_a-score_c', True),
        ]
        assert_that(expected,
                    equal_to(list(self._storage.get_partner_scores('partner_a'))))

    def test_get_partner_scores(self):
        logger.info('Start test_ydb_score_path_storage, get partner scores')
        expected = [
            PartnerScoresTable.Record('partner_a', 'score_a', 'partner_a-score_a', True),
            PartnerScoresTable.Record('partner_a', 'score_b', 'partner_a-score_b', True),
            PartnerScoresTable.Record('partner_a', 'score_c', 'partner_a-score_c', True),
        ]
        assert_that(expected,
                    equal_to(list(self._storage.get_partner_scores('partner_a'))))

    def test_get_scores_for_unknown_partner(self):
        logger.info('Start test_ydb_score_path_storage, get partner scores')
        expected = []
        assert_that(expected,
                    equal_to(list(self._storage.get_partner_scores('UNKNOWN_partner'))))
