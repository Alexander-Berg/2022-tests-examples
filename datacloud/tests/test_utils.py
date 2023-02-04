from hamcrest import assert_that, is_
import ydb
from datacloud.dev_utils.logging.logger import get_basic_logger
from datacloud.dev_utils.ydb.lib.core import utils

logger = get_basic_logger(__name__)


class TestSimple(object):
    @classmethod
    def setup_class(cls):
        with open('ydb_database.txt', 'r') as r:
            database = r.read().strip()
        with open('ydb_endpoint.txt', 'r') as r:
            endpoint = r.read().strip()
        driver_config = ydb.DriverConfig(endpoint, database)
        cls.driver = ydb.Driver(driver_config)
        cls.driver.wait(timeout=4)

    def test_is_directory_exists(self):
        d = self.driver
        path = '/local/this-folder-exists'
        assert_that(utils.is_directory_exists(d, path), is_(False))
        d.scheme_client.make_directory(path)
        assert_that(utils.is_directory_exists(d, path), is_(True))
        d.scheme_client.remove_directory(path)
        assert_that(utils.is_directory_exists(d, path), is_(False))

    def test_is_table_exists(self):
        d = self.driver
        table = '/local/this-table-exists'
        assert_that(utils.is_table_exists(d, table), is_(False))
        self._create_sample_table(table)
        assert_that(utils.is_table_exists(d, table), is_(True))
        self._drop_table(table)
        assert_that(utils.is_table_exists(d, table), is_(False))

    def test_table_is_not_folder(self):
        d = self.driver
        table = '/local/this-table-is-not-folder'
        self._create_sample_table(table)
        assert_that(utils.is_table_exists(d, table), is_(True))
        assert_that(utils.is_directory_exists(d, table), is_(False))
        self._drop_table(table)

    def test_folder_is_not_table(self):
        d = self.driver
        path = '/local/this-folder-is-not-table'
        d.scheme_client.make_directory(path)
        assert_that(utils.is_table_exists(d, path), is_(False))
        assert_that(utils.is_directory_exists(d, path), is_(True))
        d.scheme_client.remove_directory(path)

    def test_create_folder(self):
        d = self.driver
        path = '/local/test-create-folder/long/path'
        assert_that(utils.is_directory_exists(d, path), is_(False))
        utils.create_folder(d, 'local', 'test-create-folder/long/path')
        assert_that(utils.is_directory_exists(d, path), is_(True))
        d.scheme_client.remove_directory(path)
        assert_that(utils.is_directory_exists(d, path), is_(False))

    def test_existing_folder_will_stay(self):
        d = self.driver
        path = '/local/test-folder-will-stay/and-this-thin-exists'
        assert_that(utils.is_directory_exists(d, path), is_(False))
        utils.create_folder(d, 'local', 'test-folder-will-stay/and-this-thin-exists')
        assert_that(utils.is_directory_exists(d, path), is_(True))
        # we try to recreate upper folder, but nothing should happen
        # inner content should stay
        utils.create_folder(d, 'local', 'test-folder-will-stay')
        assert_that(utils.is_directory_exists(d, path), is_(True))
        d.scheme_client.remove_directory(path)
        assert_that(utils.is_directory_exists(d, path), is_(False))

    def _create_sample_table(self, path):
        session = self.driver.table_client.session().create()
        session.create_table(
            path,
            ydb.TableDescription().with_column(ydb.Column(
                'sample',
                ydb.OptionalType(ydb.DataType.Uint64))).with_primary_key('sample'))

    def _drop_table(self, path):
        session = self.driver.table_client.session().create()
        session.drop_table(path)
