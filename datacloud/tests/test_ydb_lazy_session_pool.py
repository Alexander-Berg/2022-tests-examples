from hamcrest import assert_that, is_
import ydb
from ydb.table import Session
from datacloud.dev_utils.logging.logger import get_basic_logger
from datacloud.dev_utils.ydb.lib.core.ydb_lazy_driver import YdbLazyDriver
from datacloud.dev_utils.ydb.lib.core.ydb_manager import YdbConnectionParams
from datacloud.dev_utils.ydb.lib.core.ydb_lazy_session_pool import (
    YdbLazySessionPool)
from datacloud.dev_utils.ydb.lib.core import utils

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
        cls.lazy_driver = YdbLazyDriver(cls.connection_params)

    def test_lazy_session_pool(self):
        table = '/local/table-to-test-lazy-session-pool'
        pool = YdbLazySessionPool(self.lazy_driver)
        assert_that(pool._lazy_driver, is_(YdbLazyDriver))
        assert_that(pool._session, is_(None))
        with pool.acquire() as session:
            d = self.lazy_driver.driver
            assert_that(session, is_(Session))
            assert_that(utils.is_table_exists(d, table), is_(False))
            session.create_table(
                table,
                ydb.TableDescription()
                .with_column(ydb.Column('t', ydb.OptionalType(ydb.DataType.Uint8)))
                .with_primary_key('t')
            )
            assert_that(utils.is_table_exists(d, table), is_(True))
