from hamcrest import assert_that, is_, equal_to, raises, calling
import ydb
from datacloud.dev_utils.logging.logger import get_basic_logger
from datacloud.dev_utils.ydb.lib.core.ydb_manager import YdbConnectionParams
from datacloud.dev_utils.ydb.lib.core.ydb_table import YdbTable
from datacloud.dev_utils.ydb.lib.core.ydb_manager import YdbManager
from datacloud.dev_utils.ydb.lib.core import utils

logger = get_basic_logger(__name__)


class TestBaseYdbTable(object):
    @classmethod
    def setup_class(cls):
        # initial setup of the test
        with open("ydb_database.txt", 'r') as r:
            cls.database = r.read()
        with open("ydb_endpoint.txt", 'r') as r:
            endpoint = r.read()
        cls.driver_config = ydb.DriverConfig(endpoint, cls.database)

        cls.connection_params = YdbConnectionParams(
            endpoint,
            cls.database,
            'some-random-token',)
        cls.manager = YdbManager(cls.connection_params)

    def test_base_table_creation(self):
        database = 'local'
        table_path = 'test-base-ydb-creation-folder/test-base-ydb-creation-table'
        table = YdbTable(self.manager, database, table_path)
        assert_that(table._ydb_manager, is_(YdbManager))
        assert_that(table.database, equal_to(database))
        assert_that(table.table, equal_to(table_path))
        assert_that(table.full_table_path, equal_to(database + '/' + table_path))
        assert_that(table.folder,
                    equal_to(database + '/test-base-ydb-creation-folder'))
        assert_that(table._session, is_(None))
        assert_that(calling(table.create), raises(NotImplementedError))

    def test_inherited_table(self):
        database = self.database
        table_name = 'tiny-base-table'
        table_path = 'local/tiny-base-table'
        d = self.manager.driver.driver
        table = TinyTable(self.manager, database, table_name)
        assert_that(utils.is_table_exists(d, table_path), is_(False))
        table.create()
        assert_that(utils.is_table_exists(d, table_path), is_(True))
        assert_that(table.get_one(table.Record(key=2)), is_(None))
        table.insert([table.Record(key=2, value=4)])
        assert_that(table.get_one(table.Record(key=2)).value, is_(4))
        assert_that(next(table.get(table.Record(key=2))).value, is_(4))
        with table._init_session() as session:
            session.drop_table(table_path)


# Child class for tests
class TinyTable(YdbTable):
    def __init__(self, ydb_manager, database, table):
        super(TinyTable, self).__init__(ydb_manager, database, table)

    def create(self):
        with self._init_session() as session:
            session.create_table(
                self.full_table_path,
                ydb.TableDescription()
                .with_column(
                    ydb.Column('key', ydb.OptionalType(ydb.DataType.Uint64)))
                .with_column(
                    ydb.Column('value', ydb.OptionalType(ydb.DataType.Uint64)))
                .with_primary_key('key'))

    def get_one(self, record):
        query_params = {'$key': record.key}
        return self._get_one(query_params)

    def get(self, record):
        query_params = {'$key': record.key}
        for rec in self._get(query_params):
            yield rec

    class Record(object):
        __slots__ = ('key', 'value')

        def __init__(self, key, value=None):
            self.key = key
            self.value = value

        def __eq__(self, other):
            return self.key == other.key and self.value == other.value

    _insert_request = """
        PRAGMA TablePathPrefix("{database}");

        DECLARE $records AS "List<Struct<
            key: Uint64,
            value: Uint64>>";

        REPLACE INTO [{table}]
        SELECT
            key,
            value
        FROM AS_TABLE($records);
    """

    _select_request = """
        PRAGMA TablePathPrefix("{database}");

        DECLARE $key AS Uint64;

        SELECT *
        FROM [{table}]
        WHERE key = $key;
    """

    _select_multiple_request = """
        PRAGMA TablePathPrefix("{database}");

        SELECT *
        FROM [{table}]
        WHERE key IN {items};
    """
