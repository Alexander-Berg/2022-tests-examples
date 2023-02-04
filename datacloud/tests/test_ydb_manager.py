from hamcrest import assert_that, is_
import ydb
from datacloud.dev_utils.logging.logger import get_basic_logger
from datacloud.dev_utils.ydb.lib.core import utils
from datacloud.dev_utils.ydb.lib.core.ydb_lazy_session_pool import YdbLazySessionPool
from datacloud.dev_utils.ydb.lib.core.ydb_lazy_driver import YdbLazyDriver
from datacloud.dev_utils.ydb.lib.core.ydb_manager import (
    YdbConnectionParams, YdbManager)


logger = get_basic_logger(__name__)


class TestSimple(object):
    @classmethod
    def setup_class(cls):
        with open("ydb_database.txt", 'r') as r:
            cls.database = r.read()
        with open("ydb_endpoint.txt", 'r') as r:
            endpoint = r.read()
        cls.driver_config = ydb.DriverConfig(endpoint, cls.database)

        cls.connection_params = YdbConnectionParams(
            endpoint,
            cls.database,
            'some-random-token',)

    def test_ydb_manager(self):
        path = '/local/path-to-test-ydb-manager-works'
        manager = YdbManager(self.connection_params)
        assert_that(manager.driver, is_(YdbLazyDriver))
        assert_that(manager.pool, is_(YdbLazySessionPool))
        d = manager.driver.driver
        assert_that(utils.is_directory_exists(d, path), is_(False))
        d.scheme_client.make_directory(path)
        assert_that(utils.is_directory_exists(d, path), is_(True))
        d.scheme_client.remove_directory(path)
        assert_that(utils.is_directory_exists(d, path), is_(False))
