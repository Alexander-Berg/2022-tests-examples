from hamcrest import assert_that, is_, equal_to, raises, calling
import ydb
from ydb.driver import Driver
from datacloud.dev_utils.logging.logger import get_basic_logger
from datacloud.dev_utils.ydb.lib.core.ydb_lazy_driver import YdbLazyDriver
from datacloud.dev_utils.ydb.lib.core.ydb_manager import YdbConnectionParams

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

    def test_create_lazy_driver(self):
        lazy_driver = YdbLazyDriver(self.connection_params)
        assert_that(lazy_driver._driver, is_(None))
        driver = lazy_driver.driver
        assert_that(driver, is_(Driver))
        assert_that(lazy_driver._driver, is_(Driver))
        cached_driver = lazy_driver.driver
        assert_that(driver, equal_to(cached_driver))

    def test_fail_with_timeout(self):
        wrong_connection_params = YdbConnectionParams('wrong', 'wrong', 'wrong')
        lazy_driver = YdbLazyDriver(wrong_connection_params)
        assert_that(calling(lazy_driver._get_driver), raises(RuntimeError))

    def test_driver_really_work(self):
        path = '/local/lazy-folder-exists'
        d = YdbLazyDriver(self.connection_params).driver
        assert_that(self._exists(d, path), is_(False))
        d.scheme_client.make_directory(path)
        assert_that(self._exists(d, path), is_(True))
        d.scheme_client.remove_directory(path)
        assert_that(self._exists(d, path), is_(False))

    def _exists(self, driver, path):
        try:
            return driver.scheme_client.describe_path(path).is_directory()
        except ydb.SchemeError:
            return False
