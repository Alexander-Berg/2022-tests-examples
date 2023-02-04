# -*- coding: utf-8 -*-
import ydb

from datacloud.dev_utils.ydb.lib.core.utils import YdbPathConfig
from datacloud.dev_utils.ydb.lib.core.ydb_manager import (
    YdbConnectionParams, YdbManager)
from datacloud.score_api.storage.for_tests import setup_testing


class BaseStorageTest(object):

    @classmethod
    def setup_class(cls):
        with open('ydb_database.txt', 'r') as r:
            cls._database = r.read().strip()

        with open('ydb_endpoint.txt', 'r') as r:
            endpoint = r.read().strip()

        cls.root_dir = cls._database
        cls.driver_config = ydb.DriverConfig(endpoint, cls._database)

        cls._path_config = YdbPathConfig('testing')
        cls.connection_params = YdbConnectionParams(
            endpoint,
            cls.root_dir,
            'some-random-token',)
        cls._ydb_manager = YdbManager(cls.connection_params)
        setup_testing.main(cls._ydb_manager, cls._database)
