# -*- coding: utf-8 -*-
from hamcrest import assert_that, equal_to, is_
from datacloud.dev_utils.logging.logger import get_basic_logger
from datacloud.score_api.storage.users.ydb.storage import YdbUserStorage
from datacloud.score_api.storage.for_tests.base_storage_test import BaseStorageTest

logger = get_basic_logger(__name__)


class TestYdbUserStorage(BaseStorageTest):

    user_storage_path = 'testing/config/partner_tokens'

    @classmethod
    def setup_class(cls):
        super(TestYdbUserStorage, cls).setup_class()
        cls._storage = YdbUserStorage(
            cls._ydb_manager,
            cls._database,
            TestYdbUserStorage.user_storage_path)

    def test_user_request(self):
        logger.info('Ydb User Storage test_user_request')
        test_token = 'lolohlaephacesohngaachaeseelaghepuquohweejuudoakeigeejahgeiphofo'
        test_user_id = 'partner_c'
        requested_user = self._storage.get_user_by_token(test_token)
        assert_that(test_user_id, equal_to(requested_user.id))

    def test_unexisting_user_request(self):
        logger.info('Ydb User Storage test_unexisting_user_request')
        test_token = 'unexisting-token'
        requested_user = self._storage.get_user_by_token(test_token)
        assert_that(requested_user, is_(None))
