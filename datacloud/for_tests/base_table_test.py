import os
import ydb
from datacloud.dev_utils.logging.logger import get_basic_logger
from datacloud.dev_utils.ydb.lib.core import utils as ydb_utils
from datacloud.dev_utils.ydb.lib.core.ydb_manager import (
    YdbConnectionParams, YdbManager)


logger = get_basic_logger(__name__)


class BaseTableTest(object):

    # _table = 'please-define-your-ydb-table'
    _TableClass = None
    _database = None

    # dont remove, _table is defined in child class and unknown here
    @property
    def table_path(self):
        return os.path.join(self._database, self._table)

    @classmethod
    def setup_class(cls):
        with open('ydb_endpoint.txt', 'r') as r:
            endpoint = r.read().strip()
        with open('ydb_database.txt', 'r') as r:
            cls._database = r.read().strip()

        cls.driver_config = ydb.DriverConfig(endpoint, cls._database)
        cls.connection_params = YdbConnectionParams(
            endpoint,
            cls._database,
            'some-random-token',)
        cls._ydb_manager = YdbManager(cls.connection_params)

    def _create_table_if_not_exists(self):
        d = self._ydb_manager.driver.driver
        table = self._TableClass(self._ydb_manager, self._database, self._table)
        if not ydb_utils.is_table_exists(d, self.table_path):
            ydb_utils.create_folder(d, self._database, table.folder)
        table.create()

    def _drop_table_if_exists(self):
        table = self._TableClass(self._ydb_manager, self._database, self._table)
        assert self._database in ('local', '/local'), 'WARNING: It not seems like this is test db, are you sure?? {} {}'.format(
            self._database, self._table)
        d = self._ydb_manager.driver.driver
        if ydb_utils.is_table_exists(d, self.table_path):
            with table._init_session() as session:
                session.drop_table(self.table_path)
