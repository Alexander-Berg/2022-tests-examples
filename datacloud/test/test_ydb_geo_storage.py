# -*- coding: utf-8 -*-
import os
from hamcrest import assert_that, equal_to, is_
from datacloud.dev_utils.logging.logger import get_basic_logger
from datacloud.score_api.storage.geo.ydb.storage import YdbGeoStorage
from datacloud.score_api.storage.for_tests.base_storage_test import BaseStorageTest

logger = get_basic_logger(__name__)


class TestYdbGeoStorage(BaseStorageTest):

    @classmethod
    def setup_class(cls):
        super(TestYdbGeoStorage, cls).setup_class()
        cls._storage = YdbGeoStorage(
            cls._ydb_manager,
            cls._database,
            cls._path_config.geo_path_table_path,
            cls._path_config.geo_root_path,
        )

    def test_get_geo_path_request(self):
        logger.info('Start test_get_geo_path, request')
        expected_output = os.path.join(self._path_config.geo_root_path,
                                       'test-geo-logs-table')
        assert_that(self._storage.get_geo_path('test-internal-score-name'),
                    equal_to(expected_output))

    def test_get_geo_path_unexisting_score(self):
        logger.info('Start test_get_geo_path, unexisting score')
        assert_that(self._storage.get_geo_path('NOT_REALLY_AN_INTERNAL_SCORE'),
                    is_(None))

    def test_get_crypta_path_request(self):
        logger.info('Start test_get_crypta_path, request')
        expected_output = os.path.join(self._path_config.geo_root_path,
                                       'test-crypta-table')
        assert_that(self._storage.get_crypta_path('test-internal-score-name'),
                    equal_to(expected_output))

    def test_get_crypta_path_unexisting_score(self):
        logger.info('Start test_get_crypta_path, unexisting score')
        assert_that(self._storage.get_crypta_path('NOT_REALLY_AN_INTERNAL_SCORE'),
                    is_(None))

    def test_get_weights_request(self):
        logger.info('Start test_get_weights, request')
        expected_output = [1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0, -1.0, -1.0, -1.0, -1.0, 2.0, 3.0, 4.0]
        assert_that(self._storage.get_geo_weights('test-internal-score-name'),
                    equal_to(expected_output))

    def test_get_weights_unexisting_score(self):
        logger.info('Start test_get_weights, unexisting score')
        assert_that(self._storage.get_geo_weights('NOT_REALLY_AN_INTERNAL_SCORE'),
                    is_(None))

    def test_get_hashed_cids_request(self):
        logger.info('Start test_get_hashed_cids, request')
        assert_that(self._storage.get_hashed_cids_by_id_values('test-internal-score-name', [1001]),
                    equal_to(set([1])))

    def test_get_hashed_cids_request_multiple(self):
        logger.info('Start test_get_hashed_cids, multiple')
        assert_that(self._storage.get_hashed_cids_by_id_values('test-internal-score-name', [1001, 1003, 1005]),
                    equal_to(set([1, 3, 5])))

    def test_get_hashed_cids_request_unexisting_score(self):
        logger.info('Start test_get_hashed_cids, unexisting_score')
        assert_that(self._storage.get_hashed_cids_by_id_values('NOT_REALLY_AN_INTERNAL_SCORE', [1001, 1003, 1005]),
                    is_(None))

    def test_get_hashed_cids_request_unexisting_id_1(self):
        logger.info('Start test_get_hashed_cids, unexisting_id_1')
        assert_that(self._storage.get_hashed_cids_by_id_values('test-internal-score-name', [100500]),
                    equal_to(set()))

    def test_get_hashed_cids_request_unexisting_id_2(self):
        logger.info('Start test_get_hashed_cids, unexisting_id_2')
        assert_that(self._storage.get_hashed_cids_by_id_values('test-internal-score-name', [100500, 100501]),
                    equal_to(set()))

    def test_get_hashed_cids_request_unexisting_id_3(self):
        logger.info('Start test_get_hashed_cids, unexisting_id_3')
        assert_that(self._storage.get_hashed_cids_by_id_values('test-internal-score-name', [1001, 100500, 1002]),
                    equal_to(set([1, 2])))

    def test_get_points_by_id_request(self):
        logger.info('Start test_get_points, request')
        expected_output = set(['ucftd3q09x', 'ucfv1nkr3n'])
        assert_that(self._storage.get_geo_logs_by_id_values('test-internal-score-name', [1001]),
                    equal_to(expected_output))

    def test_get_points_by_id_multiple(self):
        logger.info('Start test_get_points, multiple')
        expected_output = set(['ucftd3q09x', 'ucfv1nkr3n', 'ubkhsznuqm', 'ucfs8mkvw2', 'ucfs8mm7z8'])
        assert_that(self._storage.get_geo_logs_by_id_values('test-internal-score-name', [1001, 1003, 1004]),
                    equal_to(expected_output))

    def test_get_points_by_id_unexisting_score(self):
        logger.info('Start test_get_points, unexisting_score')
        assert_that(self._storage.get_hashed_cids_by_id_values('NOT_REALLY_AN_INTERNAL_SCORE', [1001, 1003, 1004]),
                    is_(None))

    def test_get_points_by_id_unexisting_id_1(self):
        logger.info('Start test_get_points, unexisting_id_1')
        expected_output = set()
        assert_that(self._storage.get_geo_logs_by_id_values('test-internal-score-name', [100500]),
                    equal_to(expected_output))

    def test_get_points_by_id_unexisting_id_2(self):
        logger.info('Start test_get_points, unexisting_id_2')
        expected_output = set(['ucftd3q09x', 'ucfv1nkr3n', 'ubkhsznuqm', 'ucfs8mkvw2', 'ucfs8mm7z8'])
        assert_that(self._storage.get_geo_logs_by_id_values('test-internal-score-name', [1001, 1003, 100500, 1004]),
                    equal_to(expected_output))
