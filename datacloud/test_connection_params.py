import os
from datacloud.dev_utils.ydb.lib.core.ydb_manager import YdbConnectionParams
from datacloud.score_api.storage.ydb.utils import YdbPathConfig


__all__ = [
    'TEST_CONNECTION_PARAMS_PRESET',
    'TEST_YDB_PATH_CONFIG_PRESET'
]


TEST_CONNECTION_PARAMS_PRESET = YdbConnectionParams(
    endpoint='ydb-ru-prestable.yandex.net:2135',
    database='/ru-prestable/impulse/test/xprod-scoring-api-db',
    auth_token=os.environ['YDB_TOKEN']
)


TEST_YDB_PATH_CONFIG_PRESET = YdbPathConfig(root_dir='testing_score_api')
